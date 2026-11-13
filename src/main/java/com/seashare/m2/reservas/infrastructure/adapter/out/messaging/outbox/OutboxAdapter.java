package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import com.seashare.m2.reservas.application.dto.LatePaymentReversalEvent;
import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.dto.OperationalStatusEvent;
import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.FinanceStatusMapper;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxAdapter implements OutboxPort { // adaptador de persistencia para el patron outbox

    public static final String DESTINATION_AMQP = "AMQP"; // identificador del tipo de destino amqp

    public static final String DESTINATION_M1_HTTP = "M1_HTTP"; // T-08: destino http hacia Modulo 1 (D-01)

    public static final String PAYMENT_TOKEN_FIELD = "payment_token_ref"; // campo con token de pago: se purga al confirmar (D-09)

    private final OutboxMessageJpaRepository repository; // repositorio jpa de mensajes outbox
    private final MessagingProperties messagingProperties; // nombres de topologia leidos de application.yaml
    private final ObjectMapper objectMapper = new JsonMapper(); // mapeador jackson 3 para construir el payload

    public OutboxAdapter(OutboxMessageJpaRepository repository, MessagingProperties messagingProperties) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
        this.messagingProperties = messagingProperties; // asigna propiedades de mensajeria
    }

    @Override
    public void enqueue(StatusChangedEvent event) { // encola evento de cambio de estado
        String status = FinanceStatusMapper.toFinanceStatus(event.status(), event.subtype()); // traduce al nombre del contrato UC07
        Map<String, Object> payloadMap = new LinkedHashMap<>(); // mapa ordenado para claves json
        payloadMap.put("reservation_id", event.reservationId().toString()); // clave reservation_id
        payloadMap.put("status", status); // clave status con el nombre que espera M3
        payloadMap.put("status_changed_at", event.changedAt().toString()); // clave status_changed_at en formato iso-8601
        String paymentTokenRef = event.paymentTokenRef(); // token opaco hacia M3
        if ("PENDING".equals(status) && paymentTokenRef != null && !paymentTokenRef.isBlank()) { // solo el evento pendiente lleva token
            payloadMap.put(PAYMENT_TOKEN_FIELD, paymentTokenRef); // campo con token agregado al final
        }

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

    @Override
    public void enqueue(OperationalStatusEvent event) { // T-08: notificacion de estado operativo a M1 (sin dinero, FR-007)
        Map<String, Object> payloadMap = new LinkedHashMap<>(); // mapa ordenado para claves json
        payloadMap.put("vessel_id", event.vesselId().toString()); // embarcacion en modulo 1
        payloadMap.put("operational_status", event.operationalStatus().name()); // estado operativo ya mapeado
        payloadMap.put("occurred_at", event.occurredAt().toString()); // hora exacta de la transicion iso-8601
        payloadMap.put("event_id", event.eventId().toString()); // id unico por transicion (deduplicacion)

        String payload; // variable para almacenar texto json serializado
        try { // serializacion segura con jackson 3
            payload = objectMapper.writeValueAsString(payloadMap); // genera texto json
        } catch (Exception e) { // captura cualquier excepcion de serializacion
            throw new IllegalStateException("Failed to serialize operational status payload", e); // lanza error si falla serializacion
        }

        OutboxMessageEntity entity = new OutboxMessageEntity(); // instancia entidad outbox
        entity.setEventId(event.eventId()); // asigna event id para deduplicacion
        entity.setDestination(DESTINATION_M1_HTTP); // destino http hacia modulo 1 (D-01)
        entity.setExchange(null); // M1_HTTP no usa exchange
        entity.setRoutingKey(null); // tampoco routing key
        entity.setPayload(payload); // payload json con los 4 campos del contrato
        entity.setNextAttemptAt(event.occurredAt()); // programa intento inicial
        repository.save(entity); // persiste mensaje en outbox
    }

    @Override
    public void enqueue(LatePaymentReversalEvent event) { // CU-13: reversion del cobro tardio
        Map<String, Object> payloadMap = new LinkedHashMap<>(); // mapa ordenado para claves json
        payloadMap.put("reservation_id", event.reservationId().toString()); // reserva que permanece EXPIRED
        payloadMap.put("status", "CANCELLED_FLEXIBLE"); // unico valor que M3 trata como reembolso del 100 %
        payloadMap.put("status_changed_at", event.changedAt().toString()); // marca iso-8601 en UTC

        String payload; // variable para almacenar texto json serializado
        try { // serializacion segura con jackson 3
            payload = objectMapper.writeValueAsString(payloadMap); // genera texto json
        } catch (Exception e) { // captura cualquier excepcion de serializacion
            throw new IllegalStateException("Failed to serialize late payment reversal payload", e); // falla segura
        }

        OutboxMessageEntity entity = new OutboxMessageEntity(); // instancia entidad outbox
        entity.setEventId(event.eventId()); // id deterministico para deduplicacion
        entity.setDestination(DESTINATION_AMQP); // destino amqp hacia modulo 3
        entity.setExchange(messagingProperties.getExchange()); // exchange de application.yaml
        entity.setRoutingKey(messagingProperties.getRoutingKeyStatusChanged()); // misma ruta de CU-14
        entity.setPayload(payload); // payload con el estado literal
        entity.setNextAttemptAt(event.changedAt()); // programa intento inicial
        repository.save(entity); // persiste el mensaje
    }

    @Override
    public List<PendingOutboxMessage> findPending(Instant now, int batchSize) { // lee pendientes para el relay
        return repository.findPending(now, batchSize) // reclama el lote con FOR UPDATE SKIP LOCKED
                .stream()
                .map(this::toPending) // convierte entidad a vista publica
                .toList(); // retorna lista inmutable
    }

    @Override
    public void lease(UUID messageId, Instant leasedUntil) { // oculta el mensaje de otras instancias durante el lease
        OutboxMessageEntity entity = load(messageId); // carga la fila reclamada
        entity.setNextAttemptAt(leasedUntil); // hasta entonces no vuelve a estar pendiente
        repository.save(entity); // persiste el arriendo en la transaccion del reclamo
    }

    @Override
    public void markPublished(UUID messageId, Instant publishedAt) { // marca publicado tras el confirm
        OutboxMessageEntity entity = load(messageId); // carga la fila
        entity.setPublishedAt(publishedAt); // fija published_at
        entity.setPayload(purgePaymentToken(entity.getPayload())); // purga solo el token, conserva el resto del payload
        repository.save(entity); // persiste el marcado
    }

    @Override
    public void markFailed(UUID messageId, int attempts, Instant nextAttemptAt, String lastError, boolean dead) { // registra fallo
        OutboxMessageEntity entity = load(messageId); // carga la fila
        entity.setAttempts(attempts); // guarda intentos consumidos
        entity.setNextAttemptAt(nextAttemptAt); // programa proximo intento o null si dead
        entity.setLastError(lastError); // guarda el ultimo error
        entity.setDead(dead); // marca dead en el 5º fallo
        if (dead) { // el mensaje ya no se reintentara
            entity.setPayload(purgePaymentToken(entity.getPayload())); // purga el token sin borrar el resto del payload
        }
        repository.save(entity); // persiste el estado del intento
    }

    private String purgePaymentToken(String payload) { // elimina solo payment_token_ref del json
        if (payload == null || !payload.contains(PAYMENT_TOKEN_FIELD)) { // sin token no hay nada que purgar
            return payload; // conserva el payload original sin reserializar
        }
        try {
            JsonNode root = objectMapper.readTree(payload); // arbol json del payload
            if (!(root instanceof ObjectNode object) || !object.has(PAYMENT_TOKEN_FIELD)) { // no es objeto o no tiene el campo
                return payload; // conserva el payload original
            }
            object.remove(PAYMENT_TOKEN_FIELD); // elimina unicamente el token de pago
            return objectMapper.writeValueAsString(object); // conserva el resto de campos
        } catch (Exception e) { // payload ilegible: se conserva para no perder el mensaje
            return payload; // la purga nunca rompe el marcado del outbox
        }
    }

    private OutboxMessageEntity load(UUID messageId) { // carga una fila outbox por id
        return repository.findById(messageId) // busca la entidad
                .orElseThrow(() -> new IllegalStateException("Outbox message not found: " + messageId)); // error si no existe
    }

    private PendingOutboxMessage toPending(OutboxMessageEntity entity) { // mapea entidad a vista publica
        return new PendingOutboxMessage( // construye el record
                entity.getId(), // clave primaria
                entity.getEventId(), // identificador del evento
                entity.getDestination(), // destino AMQP o M1_HTTP
                entity.getExchange(), // exchange (nulo en M1_HTTP)
                entity.getRoutingKey(), // routing key (nula en M1_HTTP)
                entity.getPayload(), // cuerpo json
                entity.getCreatedAt(), // hora de creacion
                entity.getAttempts() // intentos consumidos
        );
    }
}
