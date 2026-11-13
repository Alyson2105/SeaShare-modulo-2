package com.seashare.m2.disputa.integration;

import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.port.in.CloseExpiredDisputesUseCase;
import com.seashare.m2.disputa.application.port.in.RegisterDisputeClaimUseCase;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.infrastructure.adapter.out.messaging.DisputeMessagingProperties;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.in.PublishPendingOutboxUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.RabbitTestContainers;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeCommand;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeUseCase;
import com.seashare.m2.shared.contracts.dispute.OpenedDispute;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")
@Import({PostgresTestContainers.class, RabbitTestContainers.class})
@Transactional
class DisputePublishingIntegrationTest { // CU-18: publicacion solo en estados finales, contra broker real

    private static final Instant BASE = Instant.parse("2028-09-01T10:00:00Z");

    private static final Instant WINDOW_END = BASE.plus(Duration.ofHours(24));

    private final JsonMapper objectMapper = new JsonMapper(); // lectura de payloads

    @Autowired
    private CreateReservationUseCase createReservationUseCase;

    @Autowired
    private OpenDisputeUseCase openDisputeUseCase;

    @Autowired
    private RegisterDisputeClaimUseCase registerDisputeClaimUseCase;

    @Autowired
    private UpdateDisputeStatusUseCase updateDisputeStatusUseCase;

    @Autowired
    private CloseExpiredDisputesUseCase closeExpiredDisputesUseCase;

    @Autowired
    private PublishPendingOutboxUseCase publishPendingOutboxUseCase; // relay real de reservas (sin cambios)

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private DisputeMessagingProperties properties;

    @MockitoBean
    private FleetInfoPort fleetInfoPort;

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort;

    @MockitoBean
    private ClockPort clockPort;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
        when(clockPort.now()).thenReturn(BASE);
        amqpAdmin.getQueueProperties(properties.getQueueDispute()); // fuerza la declaracion de la topologia
    }

    private OpenedDispute openDispute() { // reserva completada + disputa PENDING
        ReservationView reservation = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2030, 9, 1),
                LocalDate.of(2030, 9, 3),
                2,
                "Titular de Prueba",
                "+573001112233",
                "titular@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("2500000.0000")));
        return openDisputeUseCase.open(new OpenDisputeCommand(reservation.reservationId(), ownerId, BASE));
    }

    private void registerClaim(OpenedDispute dispute) { // reclamo dentro de la ventana
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(1)));
        registerDisputeClaimUseCase.register(new RegisterDisputeClaimCommand(
                dispute.disputeId(), ownerId, "El timon llego con holgura critica por impacto", "structure", List.of()));
    }

    private List<Message> drainQueue() { // vacia la cola de disputa
        List<Message> messages = new ArrayList<>();
        Message message;
        while ((message = rabbitTemplate.receive(properties.getQueueDispute(), 1500)) != null) {
            messages.add(message);
        }
        return messages;
    }

    private List<Message> messagesFor(List<Message> messages, UUID disputeId) { // filtra por disputa del payload
        return messages.stream().filter(message -> disputeId.toString().equals(field(message, "dispute_id"))).toList();
    }

    private String field(Message message, String name) { // lee un campo del payload json
        try {
            JsonNode json = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
            return json.has(name) ? json.get(name).asText() : null;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Payload ilegible", e);
        }
    }

    @Test
    void pendingDisputeNeverPublishes() { // FR-001: cero mensajes en PENDING
        OpenedDispute dispute = openDispute();
        registerClaim(dispute);
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(2)));
        updateDisputeStatusUseCase.update(new UpdateDisputeStatusCommand(
                dispute.disputeId(), DisputeStatus.PENDING, UUID.randomUUID(), null, "peritaje en curso",
                DisputeActorType.ADMIN));

        publishPendingOutboxUseCase.execute(); // el relay no encuentra fila de esta disputa
        List<Message> messages = drainQueue();

        assertThat(messagesFor(messages, dispute.disputeId())).isEmpty();
    }

    @Test
    void acceptedIsPublishedAsCompletedWithoutAmounts() { // FR-002, FR-003, FR-004
        OpenedDispute dispute = openDispute();
        registerClaim(dispute);
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(2)));
        updateDisputeStatusUseCase.update(new UpdateDisputeStatusCommand(
                dispute.disputeId(), DisputeStatus.ACCEPTED, UUID.randomUUID(), "procedente", "validado",
                DisputeActorType.ADMIN));

        publishPendingOutboxUseCase.execute();
        List<Message> messages = messagesFor(drainQueue(), dispute.disputeId());

        assertThat(messages).hasSize(1);
        Message message = messages.get(0);
        assertThat(field(message, "status")).isEqualTo("COMPLETED");
        assertThat(field(message, "reservation_id")).isEqualTo(dispute.reservationId().toString());
        assertThat(field(message, "dispute_id")).isEqualTo(dispute.disputeId().toString());
        assertThat(field(message, "status_changed_at")).isEqualTo(BASE.plus(Duration.ofHours(2)).toString());
        assertThat(field(message, "event_key")).isEqualTo("disputa-" + dispute.disputeId() + "-COMPLETED");

        JsonNode payload = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertThat(payload.size()).isEqualTo(5); // exactamente el contrato de CU-18
        assertThat(payload.has("reason")).isFalse(); // CU-17 FR-004: el motivo no viaja
        assertThat(payload.has("owner_id")).isFalse(); // sin IDs de usuarios
        assertThat(payload.has("amount")).isFalse(); // sin montos

        UUID expectedEventId = UUID.nameUUIDFromBytes(
                ("disputa-" + dispute.disputeId() + "-COMPLETED").getBytes(StandardCharsets.UTF_8));
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(expectedEventId.toString());
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
    }

    @Test
    void expiryRejectionIsPublishedAsRejected() { // cierre automatico -> REJECTED
        OpenedDispute dispute = openDispute();
        when(clockPort.now()).thenReturn(WINDOW_END);
        closeExpiredDisputesUseCase.execute();

        publishPendingOutboxUseCase.execute();
        List<Message> messages = messagesFor(drainQueue(), dispute.disputeId());

        assertThat(messages).hasSize(1);
        assertThat(field(messages.get(0), "status")).isEqualTo("REJECTED");
        assertThat(field(messages.get(0), "event_key")).isEqualTo("disputa-" + dispute.disputeId() + "-REJECTED");
    }

    @Test
    void disputeQueueAndDlqAreDeclared() { // topologia de CU-18
        assertThat(amqpAdmin.getQueueProperties(properties.getQueueDispute())).isNotNull();
        assertThat(amqpAdmin.getQueueProperties(properties.dlqDispute())).isNotNull();
        assertThat(properties.getRoutingKeyDisputeUpdated()).isEqualTo("reservation.dispute.updated");
    }
}
