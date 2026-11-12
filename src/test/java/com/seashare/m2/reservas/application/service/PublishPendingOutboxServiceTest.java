package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.EventPublishException;
import com.seashare.m2.reservas.application.port.out.EventPublisherPort;
import com.seashare.m2.reservas.application.port.out.FleetNotificationOutcome;
import com.seashare.m2.reservas.application.port.out.FleetStatusNotificationPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio relay del outbox (T-03) con puertos simulados
 * y reloj fijo: backoff exacto, 5º intento -> dead, 4xx de M1 -> dead directo,
 * aislamiento entre mensajes y respeto del lote. Nunca se duerme (sin Thread.sleep).
 */
@ExtendWith(MockitoExtension.class)
class PublishPendingOutboxServiceTest { // pruebas unitarias del servicio relay del outbox

    private static final Instant NOW = Instant.parse("2026-11-07T00:00:00Z"); // reloj fijo de las pruebas

    private static final String EXCHANGE = "seashare.reservations"; // exchange de prueba

    private static final String ROUTING_KEY = "reservation.status.changed"; // routing key de prueba

    @Mock
    private OutboxPort outboxPort; // puerto outbox simulado

    @Mock
    private EventPublisherPort eventPublisherPort; // publicador amqp simulado

    @Mock
    private FleetStatusNotificationPort fleetStatusNotificationPort; // notificador de M1 simulado

    @Mock
    private ClockPort clockPort; // reloj fijo

    @Mock
    private TransactionTemplate transactionTemplate; // plantilla transaccional simulada (ejecuta en linea)

    private PublishPendingOutboxService service; // servicio bajo prueba

    @BeforeEach
    void setUp() { // configura reloj, TX en linea y backoff con jitter cero
        lenient().when(clockPort.now()).thenReturn(NOW); // reloj siempre en NOW
        lenient().doAnswer(invocation -> { // la TX solo ejecuta el callback en linea
            TransactionCallback<?> action = invocation.getArgument(0); // callback transaccional
            return action.doInTransaction(null); // sin estado transaccional real en unitario
        }).when(transactionTemplate).execute(any()); // stub de execute
        Random random = mock(Random.class); // random fijo
        lenient().when(random.nextDouble()).thenReturn(0.5); // jitter 0 -> retardos base exactos
        BackoffPolicy backoffPolicy = new BackoffPolicy(5, 1000, 5, 0.1, random); // politica de prueba
        service = new PublishPendingOutboxService( // construye el servicio con puertos simulados
                outboxPort, eventPublisherPort, fleetStatusNotificationPort,
                clockPort, transactionTemplate, backoffPolicy, 10); // lote de 10
    }

    @Test
    void backoffExactoUnoCincoVeinticincoCientoVeinticincoSegundos() { // 1/5/25/125 s tras cada fallo
        long[] expectedSeconds = {1, 5, 25, 125}; // retardos esperados en segundos
        for (int attempt = 0; attempt < 4; attempt++) { // simula los 4 primeros fallos
            PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_AMQP, attempt); // mensaje
            when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote
            doThrow(new EventPublishException("broker caido")).when(eventPublisherPort).publish(any()); // fallo

            service.execute(); // procesa el ciclo

            verify(outboxPort).markFailed( // verifica el reintento agendado
                    eq(message.id()), // del mensaje correcto
                    eq(attempt + 1), // intento incrementado
                    eq(NOW.plusSeconds(expectedSeconds[attempt])), // retardo exacto desde el reloj fijo
                    contains("AMQP sin confirmacion"), // error clasificado como amqp
                    eq(false)); // sigue vivo
        }
    }

    @Test
    void quintoFalloMarcaDeadConAlerta() { // tras el 5º fallo dead=true sin proximo intento
        PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_AMQP, 4); // 4 intentos previos
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote
        doThrow(new EventPublishException("broker caido")).when(eventPublisherPort).publish(any()); // 5º fallo

        service.execute(); // procesa el ciclo

        verify(outboxPort).markFailed( // marca dead
                eq(message.id()), // del mensaje
                eq(5), // quinto intento
                eq((Instant) null), // sin proximo intento
                anyString(), // con error registrado
                eq(true)); // dead=true
        verify(outboxPort, never()).markPublished(any(), any()); // jamas publicado
    }

    @Test
    void publicacionConfirmadaMarcaPublishedAt() { // ack del broker -> published_at con hora del reloj
        PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_AMQP, 0); // sin intentos
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote

        service.execute(); // procesa el ciclo

        verify(eventPublisherPort).publish(message); // publico el mensaje
        verify(outboxPort).markPublished(message.id(), NOW); // marca published_at con el reloj fijo
        verify(outboxPort, never()).markFailed(any(), anyInt(), any(), any(), eq(false)); // sin reintentos
    }

    @Test
    void cuarentaYCuatroDeM1VaDirectoDeadSinReintento() { // D-05: todo 4xx es permanente
        PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_M1_HTTP, 0); // destino M1
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote
        when(fleetStatusNotificationPort.notifyStatus(message)) // M1 responde 4xx
                .thenReturn(FleetNotificationOutcome.PERMANENT_FAILURE);

        service.execute(); // procesa el ciclo

        verify(outboxPort).markFailed( // dead directo en el primer intento
                eq(message.id()), // del mensaje
                eq(1), // un solo intento
                eq((Instant) null), // sin proximo intento
                contains("4xx"), // error clasificado como 4xx
                eq(true)); // dead=true
        verify(eventPublisherPort, never()).publish(any()); // nunca toca amqp
        verify(outboxPort, never()).markPublished(any(), any()); // nunca publicado
    }

    @Test
    void cincoCientosDeM1ReagendaConBackoff() { // 5xx/timeout de M1 reintenta con backoff
        PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_M1_HTTP, 0); // destino M1
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote
        when(fleetStatusNotificationPort.notifyStatus(message)) // M1 responde 5xx
                .thenReturn(FleetNotificationOutcome.RETRYABLE_FAILURE);

        service.execute(); // procesa el ciclo

        verify(outboxPort).markFailed( // reintento agendado
                eq(message.id()), // del mensaje
                eq(1), // primer intento
                eq(NOW.plusSeconds(1)), // backoff de 1 s
                contains("5xx"), // error clasificado como transitorio
                eq(false)); // sigue vivo
    }

    @Test
    void m1EntregadoMarcaPublishedAt() { // 2xx de M1 -> published_at
        PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_M1_HTTP, 0); // destino M1
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote
        when(fleetStatusNotificationPort.notifyStatus(message)) // M1 acepto
                .thenReturn(FleetNotificationOutcome.DELIVERED);

        service.execute(); // procesa el ciclo

        verify(outboxPort).markPublished(message.id(), NOW); // marca publicado con el reloj fijo
        verify(eventPublisherPort, never()).publish(any()); // sin publicacion amqp
    }

    @Test
    void falloDeUnMensajeNoDetieneALosDemas() { // aislamiento: el primero falla y el segundo se publica
        PendingOutboxMessage failing = pending(PublishPendingOutboxService.DESTINATION_AMQP, 0); // falla
        PendingOutboxMessage working = pending(PublishPendingOutboxService.DESTINATION_AMQP, 0); // tiene exito
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(failing), List.of(working), List.of()); // dos claims unitarios
        doAnswer(invocation -> { // falla solo el primero
            PendingOutboxMessage current = invocation.getArgument(0); // mensaje en curso
            if (current.eventId().equals(failing.eventId())) { // si es el primero
                throw new EventPublishException("fallo simulado"); // lanza
            }
            return null; // el segundo publica bien
        }).when(eventPublisherPort).publish(any()); // stub del publicador void

        int processed = service.execute(); // procesa el ciclo completo

        assertThat(processed).isEqualTo(2); // ambos mensajes intentados
        verify(outboxPort).markFailed(eq(failing.id()), eq(1), any(), anyString(), eq(false)); // primero reintenta
        verify(outboxPort).markPublished(working.id(), NOW); // segundo publicado a pesar del fallo previo
    }

    @Test
    void reclamaUnMensajePorTransaccion() { // el servicio pide un mensaje por claim
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of()); // sin pendientes

        service.execute(); // procesa el ciclo

        verify(outboxPort).findPending(NOW, 1); // claim unitario: el tamano del lote se controla en el bucle
    }

    @Test
    void destinoDesconocidoMarcaDeadDirecto() { // destino fuera del catalogo nunca tendra exito
        PendingOutboxMessage message = pending("OTRO_DESTINO", 0); // destino invalido
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote

        service.execute(); // procesa el ciclo

        verify(outboxPort).markFailed( // dead directo
                eq(message.id()), // del mensaje
                eq(1), // un intento
                eq((Instant) null), // sin proximo intento
                contains("Destino desconocido"), // error claro
                eq(true)); // dead=true
    }

    @Test
    void nuncaPropagaExcepcionesInesperadasDelPuerto() { // un fallo del puerto no escapa del servicio
        PendingOutboxMessage message = pending(PublishPendingOutboxService.DESTINATION_AMQP, 0); // mensaje
        when(outboxPort.findPending(any(), anyInt())).thenReturn(List.of(message), List.of()); // pendiente unico y fin de lote
        doThrow(new IllegalStateException("fallo raro")).when(outboxPort).markPublished(any(), any()); // falla el marcado

        assertThatCode(() -> service.execute()) // el ciclo no debe lanzar
                .doesNotThrowAnyException(); // silencia el error y continua
    }

    private PendingOutboxMessage pending(String destination, int attempts) { // construye un mensaje de prueba
        return new PendingOutboxMessage( // record de prueba
                UUID.randomUUID(), // id de fila
                UUID.randomUUID(), // event id
                destination, // destino bajo prueba
                EXCHANGE, // exchange
                ROUTING_KEY, // routing key
                "{\"reservation_id\":\"" + UUID.randomUUID() + "\"}", // payload json de ejemplo
                NOW.minusSeconds(60), // creado en el pasado
                attempts // intentos consumidos
        );
    }
}
