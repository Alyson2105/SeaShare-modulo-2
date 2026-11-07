package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OutboxAdapter implements OutboxPort { // adaptador de persistencia para el patron outbox

    public static final String DESTINATION_AMQP = "AMQP"; // identificador del tipo de destino amqp

    private final OutboxMessageJpaRepository repository; // repositorio jpa de mensajes outbox
    private final MessagingProperties messagingProperties; // nombres de topologia leidos de application.yaml
    private final ObjectMapper objectMapper = new JsonMapper(); // mapeador jackson 3 para construir el payload

    public OutboxAdapter(OutboxMessageJpaRepository repository, MessagingProperties messagingProperties) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
        this.messagingProperties = messagingProperties; // asigna propiedades de mensajeria
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
        entity.setExchange(messagingProperties.getExchange()); // asigna exchange de application.yaml
        entity.setRoutingKey(messagingProperties.getRoutingKeyStatusChanged()); // asigna routing key de application.yaml
        entity.setPayload(payload); // asigna payload json
        entity.setNextAttemptAt(event.changedAt()); // programa intento inicial
        repository.save(entity); // persiste mensaje en outbox
    }

    @Override
    public void enqueue(ReservationInfoProvidedEvent event) { // encola evento de informacion de reserva para modulo 3
        Map<String, Object> payloadMap = new LinkedHashMap<>(); // mapa ordenado para claves json snake_case
        payloadMap.put("reservation_id", event.reservationId().toString()); // clave reservation_id
        payloadMap.put("boat_id", event.vesselId().toString()); // clave boat_id segun contrato de modulo 3
        payloadMap.put("start_date", event.startDate().format(DateTimeFormatter.ISO_LOCAL_DATE)); // clave start_date en formato yyyy-MM-dd
        payloadMap.put("end_date", event.endDate().format(DateTimeFormatter.ISO_LOCAL_DATE)); // clave end_date en formato yyyy-MM-dd
        payloadMap.put("passengers", event.passengers()); // clave passengers como entero
        payloadMap.put("owner_id", event.ownerId().toString()); // clave owner_id obtenida de modulo 1
        payloadMap.put("max_capacity", event.maxCapacity()); // clave max_capacity como entero

        String payload; // variable para almacenar texto json serializado
        try { // serializacion segura con jackson 3
            payload = objectMapper.writeValueAsString(payloadMap); // genera texto json
        } catch (Exception e) { // captura cualquier excepcion de serializacion
            throw new IllegalStateException("Failed to serialize reservation.info.provided payload", e); // lanza error si falla serializacion
        }

        OutboxMessageEntity entity = new OutboxMessageEntity(); // instancia entidad outbox
        entity.setEventId(event.eventId()); // asigna event id deterministico para deduplicacion
        entity.setDestination(DESTINATION_AMQP); // asigna destino amqp
        entity.setExchange(messagingProperties.getExchange()); // asigna exchange de application.yaml
        entity.setRoutingKey(messagingProperties.getRoutingKeyInfoProvided()); // asigna routing key de application.yaml
        entity.setPayload(payload); // asigna cuerpo json con los 7 campos
        entity.setNextAttemptAt(event.occurredAt()); // programa intento inicial con la hora del evento
        repository.save(entity); // persiste mensaje en outbox
    }
}
