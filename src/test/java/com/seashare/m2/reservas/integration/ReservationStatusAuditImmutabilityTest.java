package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Excepción documentada a la regla "tests de integración sin JdbcTemplate" (autorizada en Hito H4):
 * el objeto bajo prueba es el trigger de PostgreSQL de inmutabilidad de reservation_status_audit (V1 L80-L89);
 * no existe camino público para mutar la auditoría, por diseño (StatusAuditPort solo inserta y la entidad es @Immutable).
 * JdbcTemplate se usa SOLO para intentar UPDATE y DELETE sobre reservation_status_audit.
 * Los datos de entrada se preparan por vías públicas: CreateReservationUseCase y ChangeReservationStatusUseCase.
 * Cada intento corre en su propia transacción (sin @Transactional en la clase): en PostgreSQL un error
 * de trigger aborta la transacción en curso, así que un intento no puede envenenar al siguiente.
 */
@SpringBootTest
@Import(PostgresTestContainers.class)
class ReservationStatusAuditImmutabilityTest {

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // prepara la reserva por camino publico

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // genera filas de auditoria

    @Autowired
    private JdbcTemplate jdbcTemplate; // EXCEPCION: solo para UPDATE/DELETE sobre reservation_status_audit

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // flota simulada

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // disponibilidad simulada

    private UUID reservationId; // reserva con filas de auditoria reales

    @BeforeEach
    void setUp() {
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);

        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(32),
                2,
                "Trigger Test",
                "3000000000",
                "trigger@test.com",
                LocalTime.of(9, 0),
                new BigDecimal("1000000.0000")
        ));
        reservationId = view.reservationId();
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), null, null, null, null, null));

        Long filas = jdbcTemplate.queryForObject(
                "select count(*) from reservation_status_audit where reservation_id = ?", Long.class, reservationId);
        assertThat(filas).isGreaterThanOrEqualTo(2); // creacion + transicion
    }

    @Test
    void updateOnAuditFailsAndRowStaysIntact() { // el trigger bloquea UPDATE y la fila queda intacta
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update reservation_status_audit set reason = 'HACKED' where reservation_id = ?", reservationId))
                .isInstanceOf(DataAccessException.class); // trigger lanza: reservation_status_audit es inmutable

        // integridad: un UPDATE condicionado al valor original debe volver a fallar (la fila sigue con sus valores)
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update reservation_status_audit set reason = reason where reservation_id = ? and reason = 'Reserva creada por CU-02'",
                reservationId))
                .isInstanceOf(DataAccessException.class); // sigue existiendo la fila original sin alterar
    }

    @Test
    void deleteOnAuditFailsAndRowStaysIntact() { // el trigger bloquea DELETE y la fila queda intacta
        assertThatThrownBy(() -> jdbcTemplate.update(
                "delete from reservation_status_audit where reservation_id = ?", reservationId))
                .isInstanceOf(DataAccessException.class); // trigger lanza antes del borrado

        Long filas = jdbcTemplate.queryForObject(
                "select count(*) from reservation_status_audit where reservation_id = ?", Long.class, reservationId);
        assertThat(filas).isGreaterThanOrEqualTo(2); // ninguna fila se perdio
    }
}
