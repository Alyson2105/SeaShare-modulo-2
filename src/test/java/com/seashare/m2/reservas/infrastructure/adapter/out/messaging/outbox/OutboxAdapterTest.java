package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
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
    void markPublishedPurgsPayloadWithPaymentToken() { // D-09: el payload con token se vacia tras confirmar
        UUID messageId = UUID.randomUUID(); // id de la fila
        Instant publishedAt = Instant.parse("2026-11-07T12:00:00Z"); // hora del confirm
        OutboxMessageEntity entity = new OutboxMessageEntity(); // entidad de prueba
        entity.setEventId(UUID.randomUUID()); // event id
        entity.setDestination("AMQP"); // destino
        entity.setPayload("{\"reservation_id\":\"abc\",\"payment_token_ref\":\"tok_123\"}"); // payload con token
        when(repository.findById(messageId)).thenReturn(Optional.of(entity)); // fila existente

        adapter.markPublished(messageId, publishedAt); // ejecuta el marcado con purga

        assertThat(entity.getPayload()).isEqualTo("{}"); // payload purgado sin cambio de esquema
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
}
