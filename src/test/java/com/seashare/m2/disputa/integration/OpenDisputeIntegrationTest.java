package com.seashare.m2.disputa.integration;

import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeCommand;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeUseCase;
import com.seashare.m2.shared.contracts.dispute.OpenedDispute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false") // sin jobs en la suite
@Import(PostgresTestContainers.class)
@Transactional
class OpenDisputeIntegrationTest { // apertura automatica contra PostgreSQL real (CU-16 FR-001)

    private static final Instant COMPLETED_AT = Instant.parse("2028-05-10T15:00:00Z");

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // siembra la reserva que exige la FK

    @Autowired
    private OpenDisputeUseCase openDisputeUseCase; // contrato interno bajo prueba

    @Autowired
    private DisputeRepositoryPort disputeRepositoryPort; // lectura de verificacion por puerto

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // propietario simulado de la reserva

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // disponibilidad simulada

    private UUID ownerId; // propietario devuelto por el modulo 1 simulado

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private ReservationView createReservation() { // reserva INITIATED valida para la FK de disputa
        return createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2030, 5, 10),
                LocalDate.of(2030, 5, 12),
                2,
                "Titular de Prueba",
                "+573001112233",
                "titular@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("3000000.0000")));
    }

    @Test
    void opensPendingDisputeWith24HourWindow() { // FR-001, FR-004, FR-005
        ReservationView reservation = createReservation();

        OpenedDispute opened = openDisputeUseCase.open(
                new OpenDisputeCommand(reservation.reservationId(), ownerId, COMPLETED_AT));

        assertThat(opened.status()).isEqualTo("PENDING");
        assertThat(opened.reservationId()).isEqualTo(reservation.reservationId());
        assertThat(opened.windowStart()).isEqualTo(COMPLETED_AT);
        assertThat(opened.windowEnd()).isEqualTo(COMPLETED_AT.plus(Duration.ofHours(24)));

        GuaranteeDispute persisted = disputeRepositoryPort.findById(opened.disputeId()).orElseThrow();
        assertThat(persisted.ownerId()).isEqualTo(ownerId);
        assertThat(persisted.status()).isEqualTo(DisputeStatus.PENDING);
        assertThat(persisted.hasClaim()).isFalse();
    }

    @Test
    void writesCreationRowInStatusHistory() { // historial de estados (CU-16 Key Entities)
        ReservationView reservation = createReservation();

        OpenedDispute opened = openDisputeUseCase.open(
                new OpenDisputeCommand(reservation.reservationId(), ownerId, COMPLETED_AT));

        List<DisputeStatusChange> history = disputeRepositoryPort.findStatusHistory(opened.disputeId());
        assertThat(history).hasSize(1);
        DisputeStatusChange creation = history.get(0);
        assertThat(creation.fromStatus()).isNull();
        assertThat(creation.toStatus()).isEqualTo(DisputeStatus.PENDING);
        assertThat(creation.actorType()).isEqualTo(DisputeActorType.SYSTEM);
        assertThat(creation.actorId()).isNull();
        assertThat(creation.occurredAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void isIdempotentPerReservation() { // D-10: devuelve la existente aunque el comando cambie
        ReservationView reservation = createReservation();
        OpenedDispute first = openDisputeUseCase.open(
                new OpenDisputeCommand(reservation.reservationId(), ownerId, COMPLETED_AT));

        OpenedDispute second = openDisputeUseCase.open(new OpenDisputeCommand(
                reservation.reservationId(), UUID.randomUUID(), COMPLETED_AT.plus(Duration.ofHours(5))));

        assertThat(second.disputeId()).isEqualTo(first.disputeId());
        assertThat(second.windowStart()).isEqualTo(first.windowStart());
        assertThat(second.windowEnd()).isEqualTo(first.windowEnd());
        assertThat(disputeRepositoryPort.findStatusHistory(first.disputeId())).hasSize(1);
    }
}
