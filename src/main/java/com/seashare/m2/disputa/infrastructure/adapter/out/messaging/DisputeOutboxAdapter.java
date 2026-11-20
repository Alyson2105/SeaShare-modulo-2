package com.seashare.m2.disputa.infrastructure.adapter.out.messaging;

import com.seashare.m2.disputa.application.dto.DisputeUpdatedEvent;
import com.seashare.m2.disputa.application.port.out.DisputeOutboxPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de salida que encola el evento de disputa en {@code outbox_message}
 * (CU-18, T-18c).
 *
 * <p>El relay del outbox de reservas publica cualquier fila de la tabla, por
 * destino y routing key leidos de la propia fila: no requiere cambios. La
 * escritura ocurre en la misma transaccion del cambio de estado; si falla, la
 * transicion completa revierte (CU-18 plan seccion 6.10).</p>
 */
@Component
public class DisputeOutboxAdapter implements DisputeOutboxPort {

    static final String DESTINATION_AMQP = "AMQP"; // destino hacia Modulo 3

    private final DisputeOutboxJpaRepository repository; // insercion de la fila

    private final DisputeMessagingProperties properties; // exchange y routing key propios del contexto

    private final JsonMapper objectMapper = new JsonMapper(); // payload json con jackson 3

    public DisputeOutboxAdapter(DisputeOutboxJpaRepository repository, DisputeMessagingProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void enqueue(DisputeUpdatedEvent event) {
        Map<String, Object> payloadMap = new LinkedHashMap<>(); // cinco campos del contrato, en orden
        payloadMap.put("reservation_id", event.reservationId().toString());
        payloadMap.put("dispute_id", event.disputeId().toString());
        payloadMap.put("status", event.financeStatus());
        payloadMap.put("status_changed_at", event.statusChangedAt().toString());
        payloadMap.put("event_key", event.eventKey());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to serialize dispute.updated payload", e);
        }

        DisputeOutboxEntity entity = new DisputeOutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventId(event.eventId()); // UNIQUE: un reenvio no duplica el evento
        entity.setDestination(DESTINATION_AMQP);
        entity.setExchange(properties.getExchange());
        entity.setRoutingKey(properties.getRoutingKeyDisputeUpdated());
        entity.setPayload(payload);
        entity.setCreatedAt(event.statusChangedAt());
        entity.setNextAttemptAt(event.statusChangedAt()); // el relay lo toma en su proximo ciclo
        repository.save(entity);
    }
}
