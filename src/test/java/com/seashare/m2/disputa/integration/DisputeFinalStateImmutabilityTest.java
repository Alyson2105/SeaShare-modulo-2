package com.seashare.m2.disputa.integration;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Excepcion documentada a la regla "tests de integracion sin JdbcTemplate" (H9, autorizada por el usuario):
 * el objeto bajo prueba es el trigger de PostgreSQL que hace inmutables los estados finales de
 * dispute_guarantee (V7); no existe camino publico para mutar una disputa final, por diseno.
 * JdbcTemplate se usa SOLO para intentar UPDATE y DELETE sobre dispute_guarantee.
 * Los datos se preparan por vias publicas: CreateReservationUseCase, OpenDisputeUseCase y
 * UpdateDisputeStatusUseCase. Sin @Transactional en la clase: un error de trigger aborta la
 * transaccion en curso, asi que cada intento corre en su propia transaccion (mismo criterio que
 * ReservationStatusAuditImmutabilityTest).
 */
@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")
@Import(PostgresTestContainers.class)
class DisputeFinalStateImmutabilityTest {

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // prepara la reserva por camino publico

    @Autowired
    private OpenDisputeUseCase openDisputeUseCase; // prepara la disputa por camino publico

    @Autowired
    private UpdateDisputeStatusUseCase updateDisputeStatusUseCase; // lleva la disputa a estado final

    @Autowired
    private JdbcTemplate jdbcTemplate; // EXCEPCION: solo para UPDATE/DELETE sobre dispute_guarantee

    @MockitoBean
    private FleetInfoPort fleetInfoPort;

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort;

    private UUID disputeId; // disputa ya en estado final

    @BeforeEach
    void setUp() {
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);

        ReservationView reservation = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(60),
                LocalDate.now().plusDays(62),
                2,
                "Trigger Disputa",
                "3000000000",
                "trigger-disputa@test.com",
                LocalTime.of(9, 0),
                new BigDecimal("1000000.0000")));

        Instant completedAt = Instant.parse("2028-08-01T10:00:00Z");
        OpenedDispute dispute = openDisputeUseCase.open(new OpenDisputeCommand(
                reservation.reservationId(), reservation.ownerId(), completedAt));

        updateDisputeStatusUseCase.update(new UpdateDisputeStatusCommand(
                dispute.disputeId(), DisputeStatus.REJECTED, null,
                GuaranteeDispute.SYSTEM_CLOSE_REASON, null, DisputeActorType.SYSTEM));

        disputeId = dispute.disputeId();
    }

    @Test
    void updateOnFinalDisputeFailsAndRowStaysIntact() { // D-19: el trigger bloquea UPDATE en estados finales
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update dispute_guarantee set status = 'PENDING' where id = ?", disputeId))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update dispute_guarantee set reason = 'HACKED' where id = ?", disputeId))
                .isInstanceOf(DataAccessException.class); // la fila sigue existiendo y sin alterar
    }

    @Test
    void deleteOnFinalDisputeFailsAndRowStaysIntact() { // D-19: tampoco se borra
        assertThatThrownBy(() -> jdbcTemplate.update(
                "delete from dispute_guarantee where id = ?", disputeId))
                .isInstanceOf(DataAccessException.class);

        Integer filas = jdbcTemplate.queryForObject(
                "select count(*) from dispute_guarantee where id = ?", Integer.class, disputeId);
        assertThat(filas).isEqualTo(1); // ninguna fila se perdio
    }
}
