package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxAdapterTest { // pruebas unitarias del adaptador outbox y formato de payload jackson 3

    @Mock
    private OutboxMessageJpaRepository repository; // simulacion del repositorio de mensajes outbox

    private OutboxAdapter adapter; // adaptador bajo prueba
    private final ObjectMapper objectMapper = new JsonMapper(); // mapeador jackson 3 para validar json

    @BeforeEach
    void setUp() { // inicializa adaptador antes de cada prueba
        adapter = new OutboxAdapter(repository, new MessagingProperties()); // propiedades con valores por defecto
    }

    @Test
    void enqueuesReservationInfoProvidedWithExactSevenFieldsAndIsoDates() throws Exception { // valida payload con 7 campos exactos y fechas yyyy-MM-dd
        UUID reservationId = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.nameUUIDFromBytes(("reservation.info.provided:" + reservationId).getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.parse("2026-11-06T15:30:00Z");

        ReservationInfoProvidedEvent event = new ReservationInfoProvidedEvent(
                eventId,
                reservationId,
                vesselId,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 15),
                6,
                ownerId,
                10,
                now
        );

        adapter.enqueue(event); // ejecuta encolado

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidad
        verify(repository).save(captor.capture()); // verifica guardado en repositorio
        OutboxMessageEntity saved = captor.getValue(); // obtiene entidad persistida

        assertThat(saved.getEventId()).isEqualTo(eventId); // verifica id deterministico
        assertThat(saved.getExchange()).isEqualTo("seashare.reservations"); // verifica exchange topic
        assertThat(saved.getRoutingKey()).isEqualTo("reservation.info.provided"); // verifica routing key
        assertThat(saved.getDestination()).isEqualTo("AMQP"); // verifica destino amqp
        assertThat(saved.getNextAttemptAt()).isEqualTo(now); // verifica proximo intento

        JsonNode json = objectMapper.readTree(saved.getPayload()); // parsea json con jackson 3
        assertThat(json.propertyNames()).containsExactlyInAnyOrder( // verifica exactamente los 7 campos
                "reservation_id", "boat_id", "start_date", "end_date", "passengers", "owner_id", "max_capacity"
        );

        assertThat(json.has("vessel_id")).isFalse(); // garantiza que vessel_id no aparece en el payload
        assertThat(json.has("departure_time")).isFalse(); // garantiza que departure_time no aparece
        assertThat(json.has("harbor_timezone")).isFalse(); // garantiza que harbor_timezone no aparece
        assertThat(json.has("estimated_total")).isFalse(); // garantiza que montos no aparecen

        assertThat(json.get("reservation_id").asText()).isEqualTo(reservationId.toString()); // verifica id de reserva
        assertThat(json.get("boat_id").asText()).isEqualTo(vesselId.toString()); // verifica boat_id con valor de vessel_id
        assertThat(json.get("start_date").asText()).isEqualTo("2027-05-10"); // verifica formato yyyy-MM-dd
        assertThat(json.get("end_date").asText()).isEqualTo("2027-05-15"); // verifica formato yyyy-MM-dd
        assertThat(json.get("start_date").asText()).matches("^\\d{4}-\\d{2}-\\d{2}$"); // verifica patron de fecha
        assertThat(json.get("end_date").asText()).matches("^\\d{4}-\\d{2}-\\d{2}$"); // verifica patron de fecha
        assertThat(json.get("passengers").asInt()).isEqualTo(6); // verifica pasajeros
        assertThat(json.get("owner_id").asText()).isEqualTo(ownerId.toString()); // verifica owner_id
        assertThat(json.get("max_capacity").asInt()).isEqualTo(10); // verifica capacidad maxima
    }

    @Test
    void enqueuesStatusChangedEventSuccessfully() { // verifica compatibilidad regresiva con status changed event
        UUID reservationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-06T15:30:00Z");

        StatusChangedEvent event = new StatusChangedEvent(
                eventId,
                reservationId,
                ReservationStatus.PENDING_PAYMENT,
                null,
                now
        );

        adapter.enqueue(event); // ejecuta encolado

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class);
        verify(repository).save(captor.capture());
        OutboxMessageEntity saved = captor.getValue();

        assertThat(saved.getExchange()).isEqualTo("seashare.reservations");
        assertThat(saved.getRoutingKey()).isEqualTo("reservation.status.changed");
        assertThat(saved.getDestination()).isEqualTo("AMQP");
    }

    @Test
    void markPublishedPurgesPaymentTokenAndKeepsRest() throws Exception { // D-09: tras confirmar solo se elimina el token
        UUID messageId = UUID.randomUUID(); // id de la fila
        Instant publishedAt = Instant.parse("2026-11-07T12:00:00Z"); // hora del confirm
        OutboxMessageEntity entity = new OutboxMessageEntity(); // entidad de prueba
        entity.setEventId(UUID.randomUUID()); // event id
        entity.setDestination("AMQP"); // destino
        entity.setPayload("{\"reservation_id\":\"abc\",\"status\":\"PENDING\",\"payment_token_ref\":\"tok_123\"}"); // payload con token
        when(repository.findById(messageId)).thenReturn(Optional.of(entity)); // fila existente

        adapter.markPublished(messageId, publishedAt); // ejecuta el marcado con purga

        JsonNode json = objectMapper.readTree(entity.getPayload()); // parsea el payload purgado
        assertThat(json.has("payment_token_ref")).isFalse(); // token eliminado
        assertThat(json.get("reservation_id").asText()).isEqualTo("abc"); // resto del payload intacto
        assertThat(json.get("status").asText()).isEqualTo("PENDING"); // estado conservado
        assertThat(entity.getPublishedAt()).isEqualTo(publishedAt); // published_at marcado
        verify(repository).save(entity); // persiste el cambio
    }

    @Test
    void markPublishedKeepsPayloadWithoutPaymentToken() { // sin token el payload no se toca
        UUID messageId = UUID.randomUUID(); // id de la fila
        Instant publishedAt = Instant.parse("2026-11-07T12:00:00Z"); // hora del confirm
        String payload = "{\"reservation_id\":\"abc\",\"status\":\"RESERVED\"}"; // payload sin token
        OutboxMessageEntity entity = new OutboxMessageEntity(); // entidad de prueba
        entity.setEventId(UUID.randomUUID()); // event id
        entity.setDestination("AMQP"); // destino
        entity.setPayload(payload); // payload sin token
        when(repository.findById(messageId)).thenReturn(Optional.of(entity)); // fila existente

        adapter.markPublished(messageId, publishedAt); // ejecuta el marcado

        assertThat(entity.getPayload()).isEqualTo(payload); // payload intacto
        assertThat(entity.getPublishedAt()).isEqualTo(publishedAt); // published_at marcado
        verify(repository).save(entity); // persiste el cambio
    }

    @Test
    void markFailedDeadPurgesPaymentTokenAndKeepsRest() throws Exception { // dead: el token ya no puede reintentarse
        UUID messageId = UUID.randomUUID(); // id de la fila
        OutboxMessageEntity entity = new OutboxMessageEntity(); // entidad de prueba
        entity.setEventId(UUID.randomUUID()); // event id
        entity.setDestination("AMQP"); // destino
        entity.setPayload("{\"reservation_id\":\"abc\",\"status\":\"PENDING\",\"payment_token_ref\":\"tok_123\"}"); // payload con token
        when(repository.findById(messageId)).thenReturn(Optional.of(entity)); // fila existente

        adapter.markFailed(messageId, 5, null, "broker caido", true); // quinto fallo: dead

        JsonNode json = objectMapper.readTree(entity.getPayload()); // parsea el payload purgado
        assertThat(json.has("payment_token_ref")).isFalse(); // token eliminado
        assertThat(json.get("reservation_id").asText()).isEqualTo("abc"); // resto del payload intacto
        assertThat(json.get("status").asText()).isEqualTo("PENDING"); // estado conservado
        assertThat(entity.isDead()).isTrue(); // mensaje marcado dead
        assertThat(entity.getAttempts()).isEqualTo(5); // intentos persistidos
        verify(repository).save(entity); // persiste el cambio
    }

    @Test
    void markFailedRetryableKeepsPaymentToken() { // un reintento vivo conserva el token
        UUID messageId = UUID.randomUUID(); // id de la fila
        String payload = "{\"reservation_id\":\"abc\",\"status\":\"PENDING\",\"payment_token_ref\":\"tok_123\"}"; // payload con token
        OutboxMessageEntity entity = new OutboxMessageEntity(); // entidad de prueba
        entity.setEventId(UUID.randomUUID()); // event id
        entity.setDestination("AMQP"); // destino
        entity.setPayload(payload); // payload con token
        when(repository.findById(messageId)).thenReturn(Optional.of(entity)); // fila existente

        adapter.markFailed(messageId, 1, Instant.parse("2026-11-07T12:01:00Z"), "timeout", false); // fallo reintentable

        assertThat(entity.getPayload()).isEqualTo(payload); // token conservado para el siguiente intento
        assertThat(entity.isDead()).isFalse(); // sigue vivo
        verify(repository).save(entity); // persiste el cambio
    }

    @Test
    void markFailedDeadKeepsPayloadWithoutPaymentToken() { // dead sin token no reescribe el payload
        UUID messageId = UUID.randomUUID(); // id de la fila
        String payload = "{\"reservation_id\":\"abc\",\"status\":\"RESERVED\"}"; // payload sin token
        OutboxMessageEntity entity = new OutboxMessageEntity(); // entidad de prueba
        entity.setEventId(UUID.randomUUID()); // event id
        entity.setDestination("AMQP"); // destino
        entity.setPayload(payload); // payload sin token
        when(repository.findById(messageId)).thenReturn(Optional.of(entity)); // fila existente

        adapter.markFailed(messageId, 5, null, "broker caido", true); // quinto fallo: dead

        assertThat(entity.getPayload()).isEqualTo(payload); // payload intacto
        assertThat(entity.isDead()).isTrue(); // mensaje marcado dead
        verify(repository).save(entity); // persiste el cambio
    }

    @Test
    void enqueuesOperationalStatusToM1HttpWithExactFourFields() throws Exception { // T-08: payload M1 del contrato CU-22
        UUID eventId = UUID.randomUUID(); // id unico por transicion
        UUID vesselId = UUID.randomUUID(); // embarcacion
        Instant occurredAt = Instant.parse("2026-11-09T12:05:00Z"); // hora de la transicion

        com.seashare.m2.reservas.application.dto.OperationalStatusEvent event =
                new com.seashare.m2.reservas.application.dto.OperationalStatusEvent(
                        eventId, vesselId, com.seashare.m2.reservas.domain.model.OperationalStatus.RESERVED, occurredAt);

        adapter.enqueue(event); // ejecuta encolado

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class);
        verify(repository).save(captor.capture());
        OutboxMessageEntity saved = captor.getValue();

        assertThat(saved.getDestination()).isEqualTo("M1_HTTP"); // destino http a M1 (D-01)
        assertThat(saved.getExchange()).isNull(); // sin exchange
        assertThat(saved.getRoutingKey()).isNull(); // sin routing key
        assertThat(saved.getNextAttemptAt()).isEqualTo(occurredAt); // primer intento en la hora del evento

        JsonNode json = objectMapper.readTree(saved.getPayload()); // parsea payload
        assertThat(json.propertyNames()).containsExactlyInAnyOrder( // exactamente los 4 campos del contrato
                "vessel_id", "operational_status", "occurred_at", "event_id"
        );
        assertThat(json.get("vessel_id").asText()).isEqualTo(vesselId.toString()); // embarcacion
        assertThat(json.get("operational_status").asText()).isEqualTo("RESERVED"); // estado operativo ya mapeado
        assertThat(json.get("occurred_at").asText()).isEqualTo("2026-11-09T12:05:00Z"); // iso-8601
        assertThat(json.get("event_id").asText()).isEqualTo(eventId.toString()); // deduplicacion
        assertThat(json.has("estimated_total")).isFalse(); // sin montos (FR-007)
        assertThat(json.has("status")).isFalse(); // sin estado crudo de reserva (SC-005)
    }

    @Test
    void statusChangedUsesFinanceStatusNames() throws Exception { // el payload usa PENDING en vez de PENDING_PAYMENT
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora del cambio

        adapter.enqueue(statusEvent(reservationId, ReservationStatus.PENDING_PAYMENT, null, null, changedAt)); // pendiente
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.RESERVED, null, null, changedAt)); // reservada
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.IN_NAVIGATION, null, null, changedAt)); // en navegacion
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.COMPLETED, null, null, changedAt)); // completada

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidades
        verify(repository, times(4)).save(captor.capture()); // cuatro encolados

        assertThat(payloadOf(captor.getAllValues().get(0)).get("status").asText()).isEqualTo("PENDING"); // nombre de M3
        assertThat(payloadOf(captor.getAllValues().get(1)).get("status").asText()).isEqualTo("RESERVED"); // sin cambio
        assertThat(payloadOf(captor.getAllValues().get(2)).get("status").asText()).isEqualTo("IN_NAVIGATION"); // sin cambio
        assertThat(payloadOf(captor.getAllValues().get(3)).get("status").asText()).isEqualTo("COMPLETED"); // sin cambio
    }

    @Test
    void cancelledPayloadUsesFinanceStatusAndOmitsSubtype() throws Exception { // cancelaciones usan los nombres UC07 y sin subtipo
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora del cambio

        adapter.enqueue(statusEvent(reservationId, ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, null, changedAt));
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.CANCELLED, CancellationSubtype.MODERATE, null, changedAt));
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.CANCELLED, CancellationSubtype.LATE, null, changedAt));
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW, null, changedAt));
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER, null, changedAt));

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidades
        verify(repository, times(5)).save(captor.capture()); // cinco encolados

        JsonNode flex = payloadOf(captor.getAllValues().get(0)); // cancelacion flexible
        JsonNode moderate = payloadOf(captor.getAllValues().get(1)); // cancelacion moderada
        JsonNode late = payloadOf(captor.getAllValues().get(2)); // cancelacion tardia
        JsonNode noShow = payloadOf(captor.getAllValues().get(3)); // inasistencia
        JsonNode byOwner = payloadOf(captor.getAllValues().get(4)); // cancelada por propietario

        assertThat(flex.get("status").asText()).isEqualTo("CANCELLED_FLEXIBLE"); // nombre de M3
        assertThat(moderate.get("status").asText()).isEqualTo("CANCELLED_MODERATE"); // nombre de M3
        assertThat(late.get("status").asText()).isEqualTo("CANCELLED_STRICT"); // nombre de M3
        assertThat(noShow.get("status").asText()).isEqualTo("CANCELLED_STRICT"); // nombre de M3
        assertThat(byOwner.get("status").asText()).isEqualTo("CANCELLED_BY_HOST"); // nombre de M3
        assertThat(flex.has("cancellation_subtype")).isFalse(); // sin subtipo en el payload
        assertThat(moderate.has("cancellation_subtype")).isFalse(); // sin subtipo en el payload
        assertThat(late.has("cancellation_subtype")).isFalse(); // sin subtipo en el payload
        assertThat(noShow.has("cancellation_subtype")).isFalse(); // sin subtipo en el payload
        assertThat(byOwner.has("cancellation_subtype")).isFalse(); // sin subtipo en el payload
    }

    @Test
    void payloadHasExactlyUc07FieldsWhenNotPending() throws Exception { // fuera de PENDING solo tres campos
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora del cambio
        StatusChangedEvent event = new StatusChangedEvent( // evento con datos que no deben publicarse
                UUID.randomUUID(), reservationId, ReservationStatus.RESERVED, null, changedAt,
                ActorType.RENTER, 120, "Novedades", "tok_ignored");

        adapter.enqueue(event); // ejecuta encolado

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidad
        verify(repository).save(captor.capture()); // un encolado
        JsonNode json = payloadOf(captor.getValue()); // parsea payload

        assertThat(json.propertyNames()).containsExactlyInAnyOrder( // exactamente los campos del contrato
                "reservation_id", "status", "status_changed_at");
    }

    @Test
    void pendingIncludesPaymentTokenRef() throws Exception { // PENDING agrega el token al final
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora del cambio

        adapter.enqueue(statusEvent(reservationId, ReservationStatus.PENDING_PAYMENT, null, "tok_abc", changedAt)); // con token

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidad
        verify(repository).save(captor.capture()); // un encolado
        JsonNode json = payloadOf(captor.getValue()); // parsea payload

        assertThat(json.propertyNames()).containsExactlyInAnyOrder( // cuatro campos con el token
                "reservation_id", "status", "status_changed_at", "payment_token_ref");
        assertThat(json.get("payment_token_ref").asText()).isEqualTo("tok_abc"); // token correcto
    }

    @Test
    void paymentTokenRefIsOmittedWhenNotPending() throws Exception { // token nulo, vacio o fuera de PENDING se omite
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora del cambio

        adapter.enqueue(statusEvent(reservationId, ReservationStatus.PENDING_PAYMENT, null, null, changedAt)); // token nulo
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.PENDING_PAYMENT, null, "   ", changedAt)); // token en blanco
        adapter.enqueue(statusEvent(reservationId, ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER, "tok_x", changedAt)); // fuera de PENDING

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidades
        verify(repository, times(3)).save(captor.capture()); // tres encolados

        assertThat(payloadOf(captor.getAllValues().get(0)).has("payment_token_ref")).isFalse(); // sin token nulo
        assertThat(payloadOf(captor.getAllValues().get(1)).has("payment_token_ref")).isFalse(); // sin token en blanco
        assertThat(payloadOf(captor.getAllValues().get(2)).has("payment_token_ref")).isFalse(); // sin token fuera de PENDING
    }

    @Test
    void payloadHasNoAmountFields() throws Exception { // el payload nunca lleva montos ni campos internos
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora del cambio
        StatusChangedEvent event = new StatusChangedEvent( // evento con todos los campos internos
                UUID.randomUUID(), reservationId, ReservationStatus.RESERVED, null, changedAt,
                ActorType.RENTER, 120, "Novedades", "tok_abc");

        adapter.enqueue(event); // ejecuta encolado

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidad
        verify(repository).save(captor.capture()); // un encolado
        JsonNode json = payloadOf(captor.getValue()); // parsea payload

        assertThat(json.has("estimated_total")).isFalse(); // sin total estimado
        assertThat(json.has("amount")).isFalse(); // sin monto
        assertThat(json.has("total")).isFalse(); // sin total
        assertThat(json.has("price")).isFalse(); // sin precio
        assertThat(json.has("cancellation_subtype")).isFalse(); // sin subtipo
        assertThat(json.has("actor")).isFalse(); // sin actor
        assertThat(json.has("anticipation_minutes")).isFalse(); // sin anticipacion
        assertThat(json.has("completion_notes")).isFalse(); // sin notas de cierre
    }

    @Test
    void statusChangedAtIsStableIso() throws Exception { // la hora del cambio se publica tal cual en iso-8601
        UUID reservationId = UUID.randomUUID(); // reserva de prueba
        Instant changedAt = Instant.parse("2026-11-09T12:05:00Z"); // hora exacta del cambio

        adapter.enqueue(statusEvent(reservationId, ReservationStatus.RESERVED, null, null, changedAt)); // ejecuta encolado

        ArgumentCaptor<OutboxMessageEntity> captor = ArgumentCaptor.forClass(OutboxMessageEntity.class); // captura entidad
        verify(repository).save(captor.capture()); // un encolado
        JsonNode json = payloadOf(captor.getValue()); // parsea payload

        assertThat(json.get("status_changed_at").asText()).isEqualTo("2026-11-09T12:05:00Z"); // iso estable
    }

    private StatusChangedEvent statusEvent(UUID reservationId,
                                           ReservationStatus status,
                                           CancellationSubtype subtype,
                                           String paymentTokenRef,
                                           Instant changedAt) { // arma un evento de cambio de estado para las pruebas
        return new StatusChangedEvent(UUID.randomUUID(), reservationId, status, subtype, changedAt,
                null, null, null, paymentTokenRef); // datos opcionales nulos salvo el token
    }

    private JsonNode payloadOf(OutboxMessageEntity entity) throws Exception { // parsea el payload json persistido
        return objectMapper.readTree(entity.getPayload()); // usa el mapeador jackson 3
    }
}
