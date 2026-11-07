package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.PublishPendingOutboxUseCase;
import com.seashare.m2.reservas.application.port.out.EventPublishException;
import com.seashare.m2.reservas.application.port.out.EventPublisherPort;
import com.seashare.m2.reservas.application.port.out.FleetStatusNotificationPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.RabbitTestContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * T-03: integracion del relay con RabbitMQ real usando solo superficie publica
 * (puertos y casos de uso). Verifica que el mensaje se publica, que
 * {@code published_at} queda marcado tras el confirm y que un fallo simulado
 * reintenta con backoff hasta publicar. Esperas con Awaitility, sin Thread.sleep.
 */
@SpringBootTest
@Import({PostgresTestContainers.class, RabbitTestContainers.class})
class OutboxRelayIntegrationTest { // integracion del relay del outbox con postgres y rabbitmq reales

    @Autowired
    private PublishPendingOutboxUseCase useCase; // caso de uso publico del relay

    @Autowired
    private OutboxPort outboxPort; // puerto publico de outbox

    @Autowired
    private AmqpAdmin amqpAdmin; // administrador amqp para purgar y contar colas

    @Autowired
    private MessagingProperties messagingProperties; // nombres de topologia

    @Autowired
    private JdbcTemplate jdbcTemplate; // acceso jdbc para verificar la tabla outbox

    @MockitoSpyBean
    private EventPublisherPort eventPublisherPort; // spy del publicador para simular fallos

    @MockitoSpyBean
    private FleetStatusNotificationPort fleetStatusNotificationPort; // spy del notificador de M1

    private final List<UUID> createdEventIds = new ArrayList<>(); // eventos creados por la prueba

    @BeforeEach
    void purgeQueues() { // deja las colas vacias para contar mensajes de este test
        amqpAdmin.purgeQueue(messagingProperties.getQueueStatus(), false); // cola de estado
        amqpAdmin.purgeQueue(messagingProperties.getQueueInfo(), false); // cola de informacion
    }

    @AfterEach
    void cleanUp() { // borra solo los outbox creados por esta clase
        for (UUID eventId : createdEventIds) {
            jdbcTemplate.update("DELETE FROM outbox_message WHERE event_id = ?", eventId); // limpia la fila
        }
        createdEventIds.clear(); // reinicia el rastreo
    }

    @Test
    void publicaEnRabbitYMarcaPublishedAt() { // mensaje AMQP publicado con confirm y marcado en outbox
        UUID eventId = enqueueStatusChanged(); // crea el pendiente

        int processed = useCase.execute(); // un solo ciclo del relay

        assertThat(processed).isEqualTo(1); // proceso el unico pendiente
        Map<String, Object> row = outboxRow(eventId); // lee la fila
        assertThat(row.get("published_at")).isNotNull(); // published_at marcado tras el confirm
        assertThat(((Number) row.get("attempts")).intValue()).isZero(); // sin fallos: intentos en 0
        assertThat(row.get("dead")).isEqualTo(false); // sigue vivo
        assertThat(statusQueueMessageCount()).isGreaterThanOrEqualTo(1L); // el mensaje llego a la cola de M3
    }

    @Test
    void falloSimuladoReintentaHastaPublicar() { // primer fallo agenda reintento y el segundo ciclo publica
        UUID eventId = enqueueStatusChanged(); // crea el pendiente
        doThrow(new EventPublishException("broker caido simulado")) // primer intento falla
                .doNothing() // segundo intento publica
                .when(eventPublisherPort).publish(any());

        await().atMost(Duration.ofSeconds(6)) // espera al reintento (backoff ~1 s con jitter)
                .pollInterval(Duration.ofMillis(400)) // sondeo rapido
                .untilAsserted(() -> { // en cada ciclo reintenta hasta publicar
                    useCase.execute(); // invoca el caso de uso publico
                    assertThat(outboxRow(eventId).get("published_at")).isNotNull(); // publicado
                });

        Map<String, Object> row = outboxRow(eventId); // verifica el estado final
        assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(1); // un fallo registrado
        assertThat(row.get("dead")).isEqualTo(false); // no murio: reintentó con exito
        assertThat(row.get("last_error")).isNotNull(); // el fallo quedo trazado en last_error
    }

    @Test
    void m1HttpUsaElPuertoFalsoYMarcaPublished() { // destino M1_HTTP sin HTTP: solo el puerto fake
        UUID eventId = UUID.randomUUID(); // event id propio
        createdEventIds.add(eventId); // rastrea para limpiar
        UUID messageId = UUID.randomUUID(); // id de la fila
        jdbcTemplate.update( // inserta un pendiente M1_HTTP directamente (hoy nada lo encola en M2)
                "INSERT INTO outbox_message (id, event_id, destination, payload, created_at, next_attempt_at, attempts, dead) "
                        + "VALUES (?, ?, 'M1_HTTP', ?, now(), ?, 0, false)",
                messageId, eventId, "{\"vessel_id\":\"" + UUID.randomUUID() + "\",\"operational_status\":\"RESERVED\"}",
                Timestamp.from(Instant.now().minusSeconds(60))); // vencido desde hace un minuto

        int processed = useCase.execute(); // un solo ciclo del relay

        assertThat(processed).isEqualTo(1); // proceso el unico pendiente
        verify(fleetStatusNotificationPort).notifyStatus(any()); // llamo al puerto de flota, sin HTTP real
        Map<String, Object> row = outboxRow(eventId); // lee la fila
        assertThat(row.get("published_at")).isNotNull(); // el fake responde OK -> publicado
        verify(eventPublisherPort, never()).publish(any()); // jamas publico en amqp
    }

    private UUID enqueueStatusChanged() { // encola un evento AMQP vencido via puerto publico
        UUID eventId = UUID.randomUUID(); // event id unico
        createdEventIds.add(eventId); // rastrea para limpiar
        outboxPort.enqueue(new StatusChangedEvent( // encola via puerto publico
                eventId, // identificador del evento
                UUID.randomUUID(), // id de reserva inventado
                ReservationStatus.PENDING_PAYMENT, // estado que genera evento outbox
                null, // sin subtipo
                Instant.parse("2026-01-01T00:00:00Z"))); // pasado: next_attempt_at vencido
        return eventId; // retorna el event id
    }

    private Map<String, Object> outboxRow(UUID eventId) { // lee la fila del outbox por event id
        return jdbcTemplate.queryForMap( // consulta unica
                "SELECT published_at, attempts, dead, last_error FROM outbox_message WHERE event_id = ?", eventId);
    }

    private long statusQueueMessageCount() { // cuenta mensajes en la cola de estado
        Properties queueProperties = amqpAdmin.getQueueProperties(messagingProperties.getQueueStatus()); // propiedades
        return ((Number) queueProperties.get("QUEUE_MESSAGE_COUNT")).longValue(); // cantidad de mensajes en cola
    }
}
