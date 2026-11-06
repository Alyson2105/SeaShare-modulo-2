package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.StatusAuditPort;
import com.seashare.m2.reservas.domain.exception.InvalidTransitionException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.state.ReservationStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class ChangeReservationStatusServiceTest { // pruebas de integracion del cambio de estado usando solo puertos y casos de uso

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso para sembrar reservas

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // caso de uso bajo prueba

    @Autowired
    private ReservationRepositoryPort reservationRepositoryPort; // puerto de lectura para verificar el estado persistido

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota de modulo 1

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad operativa

    @MockitoSpyBean
    private StatusAuditPort statusAuditPort; // espia del puerto de auditoria

    @MockitoSpyBean
    private OutboxPort outboxPort; // espia del puerto outbox

    @BeforeEach
    void setUp() { // prepara mocks de modulo 1 antes de cada prueba
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() { // restablece los espias despues de cada prueba
        Mockito.reset(statusAuditPort, outboxPort);
    }

    private Reservation createReservation() { // siembra una reserva en estado INITIATED a traves del caso de uso
        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                "Test",
                "3000000000",
                "test@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("1000000.0000")
        ));
        return reservationRepositoryPort.findById(view.reservationId()).orElseThrow();
    }

    private void changeStatus(Reservation reservation, ReservationStatus target, CancellationSubtype subtype, ActorType actor) { // ejecuta una transicion
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservation.getId(), target, subtype, actor, "Transicion de prueba"));
    }

    @Test
    void validChangeSavesStatusAndAudit() { // transicion valida persiste estado, auditoria y outbox
        Reservation created = createReservation();
        Long originalVersion = created.getVersion();

        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);

        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
        verify(statusAuditPort).record(any());
        verify(outboxPort).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void invalidChangeWritesNothing() { // transicion invalida no modifica estado ni escribe auditoria ni outbox
        Reservation created = createReservation();
        Long originalVersion = created.getVersion();

        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.COMPLETED, null, ActorType.RENTER))
                .isInstanceOf(InvalidTransitionException.class);

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.INITIATED);
        assertThat(unchanged.getVersion()).isEqualTo(originalVersion);
        verify(statusAuditPort, never()).record(any());
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void versionConflictRaisesVERSION_CONFLICT() { // guardar una copia desactualizada lanza conflicto de version
        Reservation created = createReservation();
        Reservation stale = reservationRepositoryPort.findById(created.getId()).orElseThrow();

        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);

        stale.changeStatus(ReservationStatus.EXPIRED, null, new ReservationStateMachine());

        assertThatThrownBy(() -> reservationRepositoryPort.save(stale))
                .isInstanceOf(VersionConflictException.class)
                .matches(ex -> "VERSION_CONFLICT".equals(((VersionConflictException) ex).getCode()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failureRollsEverythingBack() { // un fallo en outbox revierte el cambio de estado real en base de datos
        doThrow(new RuntimeException("boom")).when(outboxPort).enqueue(any(StatusChangedEvent.class));

        Reservation created = createReservation();
        Long originalVersion = created.getVersion();

        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.INITIATED);
        assertThat(unchanged.getVersion()).isEqualTo(originalVersion);
    }

    @Test
    void actorNotAllowedForSubtypeIsRejected() { // actor no permitido para el subtipo es rechazado sin escribir
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.RENTER);
        clearInvocations(statusAuditPort, outboxPort);

        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER, ActorType.RENTER))
                .isInstanceOf(InvalidTransitionException.class);

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        verify(statusAuditPort, never()).record(any());
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }
}
