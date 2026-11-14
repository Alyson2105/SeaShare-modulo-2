package com.seashare.m2.disputa.integration;

import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.port.in.CloseExpiredDisputesUseCase;
import com.seashare.m2.disputa.application.port.in.RegisterDisputeClaimUseCase;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
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
import com.seashare.m2.shared.domain.ClockPort;
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

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false") // el job de disputa esta apagado por defecto
@Import(PostgresTestContainers.class)
@Transactional
class DisputeWindowCloseIntegrationTest { // cierre automatico de la ventana contra PostgreSQL real (CU-16 FR-008)

    private static final Instant BASE = Instant.parse("2028-07-01T10:00:00Z");

    private static final Instant WINDOW_END = BASE.plus(Duration.ofHours(24));

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // siembra la reserva

    @Autowired
    private OpenDisputeUseCase openDisputeUseCase; // siembra la disputa

    @Autowired
    private RegisterDisputeClaimUseCase registerDisputeClaimUseCase; // reclamo cuando aplica

    @Autowired
    private UpdateDisputeStatusUseCase updateDisputeStatusUseCase; // resolucion previa cuando aplica

    @Autowired
    private CloseExpiredDisputesUseCase closeExpiredDisputesUseCase; // barrido bajo prueba

    @Autowired
    private DisputeRepositoryPort disputeRepositoryPort; // verificacion por puerto

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
    }

    private OpenedDispute openDispute() { // reserva completada + disputa PENDING con ventana desde BASE
        ReservationView reservation = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2030, 7, 1),
                LocalDate.of(2030, 7, 3),
                2,
                "Titular de Prueba",
                "+573001112233",
                "titular@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("2500000.0000")));
        return openDisputeUseCase.open(new OpenDisputeCommand(reservation.reservationId(), ownerId, BASE));
    }

    @Test
    void doesNotCloseOneSecondBeforeWindowEnd() { // borde exacto inferior: sigue PENDING
        OpenedDispute dispute = openDispute();
        when(clockPort.now()).thenReturn(WINDOW_END.minusSeconds(1));

        int closed = closeExpiredDisputesUseCase.execute();

        assertThat(closed).isZero();
        assertThat(disputeRepositoryPort.findById(dispute.disputeId()).orElseThrow().status())
                .isEqualTo(DisputeStatus.PENDING);
    }

    @Test
    void closesAtExactWindowEnd() { // now >= window_end cierra en REJECTED sin intervencion humana
        OpenedDispute dispute = openDispute();
        when(clockPort.now()).thenReturn(WINDOW_END);

        int closed = closeExpiredDisputesUseCase.execute();

        assertThat(closed).isEqualTo(1);
        GuaranteeDispute persisted = disputeRepositoryPort.findById(dispute.disputeId()).orElseThrow();
        assertThat(persisted.status()).isEqualTo(DisputeStatus.REJECTED);
        assertThat(persisted.reason()).isEqualTo(GuaranteeDispute.SYSTEM_CLOSE_REASON);
        assertThat(persisted.resolvedBy()).isNull();
        assertThat(persisted.resolvedAt()).isEqualTo(WINDOW_END);

        List<DisputeStatusChange> history = disputeRepositoryPort.findStatusHistory(dispute.disputeId());
        assertThat(history).hasSize(2); // creacion + cierre
        DisputeStatusChange close = history.get(1);
        assertThat(close.fromStatus()).isEqualTo(DisputeStatus.PENDING);
        assertThat(close.toStatus()).isEqualTo(DisputeStatus.REJECTED);
        assertThat(close.actorType()).isEqualTo(DisputeActorType.SYSTEM);
        assertThat(close.reason()).isEqualTo(GuaranteeDispute.SYSTEM_CLOSE_REASON);
    }

    @Test
    void doesNotCloseWhenClaimWasRegistered() { // el reclamo desactiva el cierre automatico
        OpenedDispute dispute = openDispute();
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(1)));
        registerDisputeClaimUseCase.register(new RegisterDisputeClaimCommand(
                dispute.disputeId(), ownerId, "El timon llego con holgura critica por impacto",
                "structure", List.of()));

        when(clockPort.now()).thenReturn(WINDOW_END.plus(Duration.ofHours(2)));
        int closed = closeExpiredDisputesUseCase.execute();

        assertThat(closed).isZero();
        assertThat(disputeRepositoryPort.findById(dispute.disputeId()).orElseThrow().status())
                .isEqualTo(DisputeStatus.PENDING);
    }

    @Test
    void doesNotCloseAlreadyResolvedDispute() { // un estado final no se toca
        OpenedDispute dispute = openDispute();
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(1)));
        registerDisputeClaimUseCase.register(new RegisterDisputeClaimCommand(
                dispute.disputeId(), ownerId, "El timon llego con holgura critica por impacto", "structure", List.of()));
        updateDisputeStatusUseCase.update(new UpdateDisputeStatusCommand(
                dispute.disputeId(), DisputeStatus.REJECTED, UUID.randomUUID(),
                "desgaste normal", null, DisputeActorType.ADMIN));

        when(clockPort.now()).thenReturn(WINDOW_END.plus(Duration.ofHours(1)));
        int closed = closeExpiredDisputesUseCase.execute();

        assertThat(closed).isZero();
        GuaranteeDispute persisted = disputeRepositoryPort.findById(dispute.disputeId()).orElseThrow();
        assertThat(persisted.status()).isEqualTo(DisputeStatus.REJECTED);
        assertThat(persisted.reason()).isEqualTo("desgaste normal");
    }
}
