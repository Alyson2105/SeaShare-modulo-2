package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.PublishPendingOutboxUseCase;
import com.seashare.m2.reservas.application.port.out.EventPublisherPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.RabbitTestContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * T-04: deduplicacion por event_id / Message-Id.
 *
 * <p>El consumidor de este archivo es SOLO DE PRUEBA (en src/test): no existe
 * ningun consumidor de produccion en M2, la deduplicacion es responsabilidad de
 * M3 (D-04). El test publica el mismo evento dos veces y comprueba que se
 * procesa una sola vez.</p>
 */
@SpringBootTest
@Import({PostgresTestContainers.class, RabbitTestContainers.class, EventDeduplicationTest.DedupConsumerConfig.class})
class EventDeduplicationTest { // prueba de deduplicacion de eventos con consumidor de prueba

    /**
     * Configuracion de prueba que registra un consumidor AMQP en la cola de
     * estado. Descarta los event_id ya vistos (set en memoria) y cuenta
     * cuantas copias llegaron y cuantas se procesaron.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class DedupConsumerConfig { // consumidor de prueba, fuera de produccion

        @Bean
        DedupConsumer dedupConsumer() { // expone el consumidor como bean del contexto de prueba
            return new DedupConsumer(); // instancia con estado en memoria
        }
    }

    /** Consumidor de prueba: deduplica por Message-Id (= event_id). */
    static class DedupConsumer { // logica de deduplicacion usada solo en pruebas

        private final Set<String> seen = ConcurrentHashMap.newKeySet(); // event_id ya procesados

        private final Map<String, Integer> received = new ConcurrentHashMap<>(); // copias recibidas por id

        private final Map<String, Integer> processed = new ConcurrentHashMap<>(); // procesamientos por id

        private final Map<String, MessageProperties> propertiesById = new ConcurrentHashMap<>(); // props por id

        @RabbitListener(queues = "${m2.messaging.queue-status}") // cola de estado de M3
        public void onMessage(Message message) { // llega un sobre del broker
            String messageId = message.getMessageProperties().getMessageId(); // Message-Id = event_id
            received.merge(messageId, 1, Integer::sum); // cuenta la copia recibida
            propertiesById.put(messageId, message.getMessageProperties()); // guarda propiedades para aserciones
            if (seen.add(messageId)) { // primera vez que se ve este event_id
                processed.merge(messageId, 1, Integer::sum); // procesa una sola vez
            }
            // copia repetida: se ignora sin procesar de nuevo (deduplicacion)
        }

        int received(String messageId) { // copias recibidas de un event id
            return received.getOrDefault(messageId, 0); // 0 si no llego nada
        }

        int processed(String messageId) { // procesamientos efectivos de un event id
            return processed.getOrDefault(messageId, 0); // 0 si no se proceso
        }

        MessageProperties properties(String messageId) { // propiedades del sobre recibido
            return propertiesById.get(messageId); // puede ser nulo si no llego
        }

        void reset() { // limpia el estado entre pruebas
            seen.clear(); // olvida los ids procesados
            received.clear(); // olvida las copias
            processed.clear(); // olvida los procesamientos
            propertiesById.clear(); // olvida las propiedades
        }
    }

    @Autowired
    private PublishPendingOutboxUseCase useCase; // caso de uso publico del relay

    @Autowired
    private OutboxPort outboxPort; // puerto publico de outbox

    @Autowired
    private EventPublisherPort eventPublisherPort; // publicador amqp real (con Message-Id)

    @Autowired
    private AmqpAdmin amqpAdmin; // para purgar la cola de prueba

    @Autowired
    private MessagingProperties messagingProperties; // nombres de topologia

    @Autowired
    private DedupConsumer dedupConsumer; // consumidor de prueba

    @Autowired
    private JdbcTemplate jdbcTemplate; // para limpiar filas outbox

    private final List<UUID> createdEventIds = new ArrayList<>(); // eventos creados por la prueba

    @BeforeEach
    void setUp() { // deja cola y consumidor limpios
        amqpAdmin.purgeQueue(messagingProperties.getQueueStatus(), false); // cola de estado vacia
        dedupConsumer.reset(); // estado del consumidor limpio
    }

    @AfterEach
    void cleanUp() { // borra solo los outbox creados por esta clase
        for (UUID eventId : createdEventIds) {
            jdbcTemplate.update("DELETE FROM outbox_message WHERE event_id = ?", eventId); // limpia la fila
        }
        createdEventIds.clear(); // reinicia el rastreo
    }

    @Test
    void publicarElMismoEventoDosVecesSeProcesaUnaSolaVez() { // at-least-once + dedup por Message-Id
        UUID eventId = enqueueStatusChanged(); // crea el evento en el outbox
        PendingOutboxMessage message = findPending(eventId); // lee el mensaje pendiente

        useCase.execute(); // primera publicacion via relay (publico)
        eventPublisherPort.publish(message); // segunda copia identica del mismo evento (redelivery simulado)

        await().atMost(Duration.ofSeconds(5)) // espera las dos copias en el consumidor
                .pollInterval(Duration.ofMillis(100)) // sondeo rapido
                .until(() -> dedupConsumer.received(eventId.toString()) == 2); // llegaron ambas copias

        assertThat(dedupConsumer.processed(eventId.toString())) // el consumidor de prueba
                .isEqualTo(1); // proceso el evento una sola vez

        MessageProperties properties = dedupConsumer.properties(eventId.toString()); // sobre recibido
        assertThat(properties.getMessageId()).isEqualTo(eventId.toString()); // Message-Id = event_id
        assertThat(properties.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON); // application/json
        assertThat(properties.getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT); // persistente
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

    private PendingOutboxMessage findPending(UUID eventId) { // localiza el mensaje encolado
        return outboxPort.findPending(Instant.now(), 50) // pendientes actuales
                .stream()
                .filter(message -> message.eventId().equals(eventId)) // del event id buscado
                .findFirst() // primero que coincida
                .orElseThrow(() -> new AssertionError("Mensaje pendiente no encontrado: " + eventId)); // o error
    }
}
