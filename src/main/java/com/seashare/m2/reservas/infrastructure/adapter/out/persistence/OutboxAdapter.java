package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.StatusChangedEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OutboxAdapter implements OutboxPort { // adaptador de persistencia para el patron outbox

    public static final String EXCHANGE = "seashare.reservations"; // exchange rabbitmq para eventos de reserva
    public static final String ROUTING_KEY = "reservation.status.changed"; // routing key del evento de cambio de estado
    public static final String DESTINATION_AMQP = "AMQP"; // identificador del tipo de destino amqp

    private final OutboxMessageJpaRepository repository; // repositorio jpa de mensajes outbox
    private final ObjectMapper objectMapper = new JsonMapper(); // mapeador jackson 3 para construir el payload

    public OutboxAdapter(OutboxMessageJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void enqueue(StatusChangedEvent event) { // encola evento de cambio de estado
        Map<String, Object> payloadMap = new LinkedHashMap<>(); // mapa ordenado para claves json
        payloadMap.put("reservation_id", event.reservationId().toString()); // clave reservation_id
        payloadMap.put("status", event.status().name()); // clave status con nombre interno
        if (event.subtype() != null) { // incluye clave solo si el subtipo no es nulo
            payloadMap.put("cancellation_subtype", event.subtype().name()); // clave cancellation_subtype
        }
        payloadMap.put("status_changed_at", event.changedAt().toString()); // clave status_changed_at en formato iso-8601

        String payload; // variable para almacenar texto json serializado
        try { // serializacion segura con jackson 3
            payload = objectMapper.writeValueAsString(payloadMap); // genera texto json
        } catch (Exception e) { // captura cualquier excepcion de serializacion
            throw new IllegalStateException("Failed to serialize outbox event payload", e); // lanza error si falla serializacion
        }

        OutboxMessageEntity entity = new OutboxMessageEntity(); // instancia entidad outbox
        entity.setEventId(event.eventId()); // asigna event id para deduplicacion
        entity.setDestination(DESTINATION_AMQP); // asigna destino amqp
        entity.setExchange(EXCHANGE); // asigna exchange
        entity.setRoutingKey(ROUTING_KEY); // asigna routing key
        entity.setPayload(payload); // asigna payload json
        entity.setNextAttemptAt(event.changedAt()); // programa intento inicial
        repository.save(entity); // persiste mensaje en outbox
    }
}
