package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.CancellationEventRecord;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.in.GetInternalReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.CancellationEventPort;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresTestContainers.class)
@Transactional
class InternalReservationReadIntegrationTest { // pruebas de integracion del lector jdbc interno con postgres real

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // siembra reservas reales

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // ejecuta transiciones reales de CU-08

    @Autowired
    private GetInternalReservationUseCase getInternalReservationUseCase; @Autowired private jakarta.persistence.EntityManager entityManager; // caso de uso bajo prueba y volcado a la base antes de leer con jdbc

    @Autowired
    private CancellationEventPort cancellationEventPort; // evento inmutable real de CU-04

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad

    @BeforeEach
    void setUp() { // prepara los mocks de modulo 1
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private ReservationView createReservation(LocalDate startDate, LocalDate endDate) { // siembra una reserva INITIATED
        return createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                startDate,
                endDate,
                2,
                "Test",
                "3000000000",
                "test@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("1000000.0000")
        ));
    }

    @Test
    void readsReservationCodeAndDurationsFromDatabase() { // lee codigo real y calcula 6 dias y 5 noches
        ReservationView created = createReservation(LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15));

        entityManager.flush(); InternalReservationView view = getInternalReservationUseCase.execute(created.reservationId());

        assertThat(view.reservationCode()).isNotBlank(); // el codigo lo genera el adaptador de escritura
        assertThat(view.durationDays()).isEqualTo(6); // ultimo dia incluido
        assertThat(view.durationNights()).isEqualTo(5); // dias menos uno
        assertThat(view.status()).isEqualTo(ReservationStatus.INITIATED); // estado real persistido
        assertThat(view.ttlApplies()).isTrue(); // INITIATED lleva temporizador
    }

    @Test
    void cancelledByOwnerIsReportedWithOwnerActor() { // la auditoria real aporta el actor OWNER
        ReservationView created = createReservation(LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 12));

        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.reservationId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), null, null, null, null, null, null));
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.reservationId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM, "Pago confirmado",
                null, null, null, null, null, "pg-1234", null));
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.reservationId(), ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER, ActorType.OWNER,
                "Averia del propietario", UUID.randomUUID(), null, true, null, null, null, null));
        cancellationEventPort.save(new CancellationEventRecord(created.reservationId(), ActorType.OWNER,
                java.time.Instant.parse("2027-06-01T09:00:00Z"), 0, CancellationSubtype.BY_OWNER,
                "mechanical_failure", null)); // cero es marcador interno, no anticipacion calculada

        entityManager.flush(); InternalReservationView view = getInternalReservationUseCase.execute(created.reservationId());

        assertThat(view.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(view.cancellationSubtype()).isEqualTo(CancellationSubtype.BY_OWNER);
        assertThat(view.cancellationActor()).isEqualTo("OWNER"); // actor leido de reservation_status_audit
        assertThat(view.cancellationAnticipationMinutes()).isNull(); // OWNER no calcula anticipacion
    }

    @Test
    void cancelledByRenterReportsPersistedAnticipation() { // cancellation_event aporta la anticipacion real
        ReservationView created = createReservation(LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 12));

        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.reservationId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), null, null, null, null, null, null));
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.reservationId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM, "Pago confirmado",
                null, null, null, null, null, "pg-1234", null));
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.reservationId(), ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, ActorType.RENTER,
                "Cambio de planes", UUID.randomUUID(), null, null, 4320, null, null, null));
        cancellationEventPort.save(new CancellationEventRecord(created.reservationId(), ActorType.RENTER,
                java.time.Instant.parse("2027-06-01T09:00:00Z"), 4320, CancellationSubtype.FLEXIBLE, null, "Cambio de planes"));

        entityManager.flush();
        InternalReservationView view = getInternalReservationUseCase.execute(created.reservationId());

        assertThat(view.cancellationAnticipationMinutes()).isEqualTo(4320L); // 72 horas en minutos
    }
}
