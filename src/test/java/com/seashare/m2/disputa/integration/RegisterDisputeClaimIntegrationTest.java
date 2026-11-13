package com.seashare.m2.disputa.integration;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.in.RegisterDisputeClaimUseCase;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false") // sin jobs en la suite
@Import(PostgresTestContainers.class)
@Transactional
class RegisterDisputeClaimIntegrationTest { // reclamo contra PostgreSQL real con reloj fijo (CU-16 FR-005/FR-006/FR-009)

    private static final Instant BASE = Instant.parse("2028-06-01T10:00:00Z");

    private static final Instant WINDOW_END = BASE.plus(Duration.ofHours(24));

    private static final String DESCRIPTION = "El timon llego con holgura critica por impacto";

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // siembra la reserva

    @Autowired
    private OpenDisputeUseCase openDisputeUseCase; // siembra la disputa

    @Autowired
    private RegisterDisputeClaimUseCase registerDisputeClaimUseCase; // caso de uso bajo prueba

    @Autowired
    private DisputeRepositoryPort disputeRepositoryPort; // verificacion por puerto

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // propietario simulado

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // disponibilidad simulada

    @MockitoBean
    private ClockPort clockPort; // reloj fijo para los bordes de la ventana

    @BeforeEach
    void setUp() {
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
        when(clockPort.now()).thenReturn(BASE);
    }

    private record Seeded(OpenedDispute dispute, UUID ownerId) { // datos de la siembra
    }

    private Seeded openDispute() { // reserva completada + disputa PENDING con ventana desde BASE
        ReservationView reservation = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2030, 6, 1),
                LocalDate.of(2030, 6, 3),
                2,
                "Titular de Prueba",
                "+573001112233",
                "titular@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("2500000.0000")));
        OpenedDispute dispute = openDisputeUseCase.open(
                new OpenDisputeCommand(reservation.reservationId(), reservation.ownerId(), BASE));
        return new Seeded(dispute, reservation.ownerId());
    }

    private RegisterDisputeClaimCommand command(Seeded seeded, UUID claimant) {
        return new RegisterDisputeClaimCommand(seeded.dispute().disputeId(), claimant, DESCRIPTION,
                "structure", List.of("https://cdn/evidencia-1.jpg"));
    }

    private void assertCode(Runnable action, String expectedCode) { // verifica el codigo del catalogo
        assertThatThrownBy(action::run)
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo(expectedCode));
    }

    @Test
    void registersClaimInsideWindowAndKeepsPending() { // FR-006
        Seeded seeded = openDispute();
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(1)));

        DisputeClaimView view = registerDisputeClaimUseCase.register(command(seeded, seeded.ownerId()));

        assertThat(view.status()).isEqualTo(DisputeStatus.PENDING);
        assertThat(view.autoCloseDisabled()).isTrue();
        assertThat(view.windowEnd()).isEqualTo(WINDOW_END);

        GuaranteeDispute persisted = disputeRepositoryPort.findById(seeded.dispute().disputeId()).orElseThrow();
        assertThat(persisted.hasClaim()).isTrue();
        assertThat(persisted.status()).isEqualTo(DisputeStatus.PENDING);

        DisputeClaim claim = disputeRepositoryPort.findClaimByDisputeId(seeded.dispute().disputeId()).orElseThrow();
        assertThat(claim.description()).isEqualTo(DESCRIPTION);
        assertThat(claim.category().wireValue()).isEqualTo("structure");
        assertThat(claim.evidenceUrls()).containsExactly("https://cdn/evidencia-1.jpg");
        assertThat(claim.registeredAt()).isEqualTo(BASE.plus(Duration.ofHours(1)));
    }

    @Test
    void acceptsClaimOneSecondBeforeWindowEnd() { // borde exacto: now = window_end - 1 s
        Seeded seeded = openDispute();
        when(clockPort.now()).thenReturn(WINDOW_END.minusSeconds(1));

        DisputeClaimView view = registerDisputeClaimUseCase.register(command(seeded, seeded.ownerId()));

        assertThat(view.status()).isEqualTo(DisputeStatus.PENDING);
    }

    @Test
    void rejectsClaimAtExactWindowEnd() { // borde exacto: now = window_end -> 409 y sin cambios
        Seeded seeded = openDispute();
        when(clockPort.now()).thenReturn(WINDOW_END);

        assertCode(() -> registerDisputeClaimUseCase.register(command(seeded, seeded.ownerId())),
                "WINDOW_EXPIRED");

        GuaranteeDispute persisted = disputeRepositoryPort.findById(seeded.dispute().disputeId()).orElseThrow();
        assertThat(persisted.hasClaim()).isFalse();
        assertThat(disputeRepositoryPort.findClaimByDisputeId(seeded.dispute().disputeId())).isEmpty();
    }

    @Test
    void rejectsSecondClaim() { // D-08
        Seeded seeded = openDispute();
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(1)));
        registerDisputeClaimUseCase.register(command(seeded, seeded.ownerId()));

        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(2)));
        assertCode(() -> registerDisputeClaimUseCase.register(command(seeded, seeded.ownerId())),
                "CLAIM_ALREADY_EXISTS");
    }

    @Test
    void rejectsClaimFromDifferentOwner() { // solo el propietario registrado
        Seeded seeded = openDispute();
        when(clockPort.now()).thenReturn(BASE.plus(Duration.ofHours(1)));

        assertCode(() -> registerDisputeClaimUseCase.register(command(seeded, UUID.randomUUID())),
                "RESERVATION_NOT_OWNED");
    }

    @Test
    void unknownDisputeIsNotFound() { // 404
        assertThatThrownBy(() -> registerDisputeClaimUseCase.register(
                new RegisterDisputeClaimCommand(UUID.randomUUID(), UUID.randomUUID(), DESCRIPTION, null, List.of())))
                .isInstanceOf(DisputeNotFoundException.class);
    }
}
