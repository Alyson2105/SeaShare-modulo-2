package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.OperationalStatusEvent;
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
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.state.ReservationStateMachine;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@Import(PostgresTestContainers.class)
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
                reservation.getId(), target, subtype, actor, "Transicion de prueba",
                actor == ActorType.SYSTEM ? null : reservation.getRenterId(), null, null, null, null, null)); // actor_id humano para cumplir CU-08
    }

    @Test
    void validChangeSavesStatusAndAudit() { // transicion valida persiste estado, auditoria y outbox
        Reservation created = createReservation();
        Long originalVersion = created.getVersion();
        clearInvocations(statusAuditPort, outboxPort); // aísla de la auditoria de creacion (T-10)

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
        clearInvocations(statusAuditPort, outboxPort); // aísla de la auditoria de creacion (T-10)

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

    @Test
    void reservedFromPendingPaymentWithPaymentReference() { // escenario 3 del spec: pago confirmado guarda la referencia
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        clearInvocations(statusAuditPort, outboxPort);

        ChangeReservationStatusResult result = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM, "Pago confirmado",
                null, null, null, null, null, "pg-8841"));

        assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(result.cancellationSubtype()).isNull();
        assertThat(result.version()).isGreaterThan(created.getVersion());
        assertThat(result.ttlExpiresAt()).isEqualTo(created.getTtlExpiresAt());
        assertThat(result.statusChangedAt()).isNotNull();
        assertThat(result.eventsEnqueued()).containsExactly("AMQP", "M1_HTTP"); // M3 y bloqueo en M1 (CU-22 FR-002)
        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        verify(statusAuditPort).record(any());
    }

    @Test
    void cancelledByRenterStoresSubtypeAndAnticipation() { // escenario 6 del spec: cancelacion con subtipo y anticipacion
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);
        clearInvocations(outboxPort);

        ChangeReservationStatusResult result = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, ActorType.RENTER, "Viaje cancelado",
                UUID.randomUUID(), null, null, 4320, null, null));

        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.cancellationSubtype()).isEqualTo(CancellationSubtype.FLEXIBLE);
        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.getCancellationSubtype()).isEqualTo(CancellationSubtype.FLEXIBLE);
        ArgumentCaptor<StatusChangedEvent> captor = ArgumentCaptor.forClass(StatusChangedEvent.class);
        verify(outboxPort).enqueue(captor.capture());
        assertThat(captor.getValue().anticipationMinutes()).isEqualTo(4320); // anticipacion transportada al evento (CU-14 FR-003)
        assertThat(captor.getValue().actor()).isEqualTo(ActorType.RENTER);
    }

    @Test
    void cancelledByOwnerRequiresMechanicalFailure() { // mecanica obligatoria al cancelar OWNER (plan L46)
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);
        UUID ownerId = UUID.randomUUID();

        assertThatThrownBy(() -> changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER, ActorType.OWNER, "Averia",
                ownerId, null, null, null, null, null)))
                .isInstanceOf(ReservationRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PARAMETERS");

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void pendingPaymentToPaymentFailedStoresReason() { // escenario 8 del spec: motivo de rechazo en la auditoria
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        clearInvocations(statusAuditPort, outboxPort);

        ChangeReservationStatusResult result = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.PAYMENT_FAILED, null, ActorType.SYSTEM, "REJECTED: fondos insuficientes",
                null, null, null, null, null, null));

        assertThat(result.status()).isEqualTo(ReservationStatus.PAYMENT_FAILED);
        assertThat(result.eventsEnqueued()).isEmpty(); // PAYMENT_FAILED no se publica (CU-14 FR-001)
        verify(statusAuditPort).record(org.mockito.ArgumentMatchers.argThat(entry ->
                "REJECTED: fondos insuficientes".equals(entry.reason())));
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void cancelFromPendingPaymentIsRejected() { // spec L156-L159: cancelacion activa rechazada con explicacion
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        clearInvocations(statusAuditPort, outboxPort);

        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, ActorType.RENTER))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("RESERVED");

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(statusAuditPort, never()).record(any());
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void checkInAndCheckOutTransitionsWork() { // escenarios 4 y 5 del spec: check-in y cierre con novedades
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);

        ChangeReservationStatusResult inNavigation = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.IN_NAVIGATION, null, ActorType.OWNER, "Check-in",
                UUID.randomUUID(), null, null, null, null, null));
        assertThat(inNavigation.status()).isEqualTo(ReservationStatus.IN_NAVIGATION);
        assertThat(inNavigation.eventsEnqueued()).containsExactly("AMQP", "M1_HTTP"); // M3 y M1 (check-in)

        ChangeReservationStatusResult completed = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.COMPLETED, null, ActorType.OWNER, "Check-out",
                UUID.randomUUID(), null, null, null, "Novedades del cierre", null));
        assertThat(completed.status()).isEqualTo(ReservationStatus.COMPLETED);
        ArgumentCaptor<StatusChangedEvent> captor = ArgumentCaptor.forClass(StatusChangedEvent.class);
        verify(outboxPort, org.mockito.Mockito.atLeastOnce()).enqueue(captor.capture());
        assertThat(captor.getAllValues().get(captor.getAllValues().size() - 1).completionNotes())
                .isEqualTo("Novedades del cierre"); // ultimo evento encolado lleva las novedades (FR-009)
    }

    @Test
    void expectedVersionMismatchIsRejected() { // expected_version del invocador no coincide -> VERSION_CONFLICT
        Reservation created = createReservation();

        assertThatThrownBy(() -> changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), 99L, null, null, null, null)))
                .isInstanceOf(VersionConflictException.class)
                .hasFieldOrPropertyWithValue("code", "VERSION_CONFLICT");

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.INITIATED);
    }

    @Test
    void missingActorIdForHumanActorIsRejected() { // actor_id obligatorio si actor no es SYSTEM (plan L44)
        Reservation created = createReservation();

        assertThatThrownBy(() -> changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                null, null, null, null, null, null)))
                .isInstanceOf(ReservationRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PARAMETERS");

        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void paymentReferenceOnlyOnReservedAndCompletionNotesOnlyOnCompleted() { // campos restringidos a su destino
        Reservation created = createReservation();

        assertThatThrownBy(() -> changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), null, null, null, null, "pg-123")))
                .isInstanceOf(ReservationRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PARAMETERS");

        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);

        assertThatThrownBy(() -> changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.IN_NAVIGATION, null, ActorType.OWNER, "Check-in",
                UUID.randomUUID(), null, null, null, "notas indebidas", null)))
                .isInstanceOf(ReservationRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PARAMETERS");
    }

    @Test
    void subtypeProhibitedOutsideCancelled() { // cancellation_subtype prohibido con los demas estados (plan L58)
        Reservation created = createReservation();

        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.PENDING_PAYMENT, CancellationSubtype.LATE, ActorType.RENTER))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void expiredTransitionDoesNotEnqueueEvents() { // EXPIRED no genera evento (matriz de efectos)
        Reservation created = createReservation();

        ChangeReservationStatusResult result = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.EXPIRED, null, ActorType.SYSTEM, "TTL expirado"));

        assertThat(result.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(result.eventsEnqueued()).isEmpty();
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void persistsPaymentReferenceAndConfirmationOnReserved() { // T-09: referencia y confirmacion de pago persistidas
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);

        ChangeReservationStatusResult result = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM, "Pago confirmado",
                null, null, null, null, null, "pg-7777"));

        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.getPaymentReference()).isEqualTo("pg-7777"); // payment_reference persistida
        assertThat(updated.getPaymentConfirmedAt()).isEqualTo(result.statusChangedAt()); // pago confirmado a la hora del cambio
        assertThat(updated.getStatusChangedAt()).isEqualTo(result.statusChangedAt()); // status_changed_at (V4) persistida
    }

    @Test
    void persistsCompletionNotesOnCompleted() { // T-09: notas de cierre persistidas por puertos
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);
        changeStatus(created, ReservationStatus.IN_NAVIGATION, null, ActorType.OWNER);

        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.COMPLETED, null, ActorType.OWNER, "Check-out",
                UUID.randomUUID(), null, null, null, "Sin novedades de desembarque", null));

        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.getCompletionNotes()).isEqualTo("Sin novedades de desembarque"); // notas persistidas
        assertThat(updated.getPaymentReference()).isNull(); // nada tocado en payment_reference
        assertThat(updated.getActualDepartureAt()).isNull(); // horas reales fuera del alcance de CU-08 (T-17c/T-17d)
    }

    @Test
    void creationWritesAuditRow() { // T-10: la creacion genera la primera fila del historial
        Reservation created = createReservation(); // la creacion llama a statusAuditPort

        verify(statusAuditPort).record(org.mockito.ArgumentMatchers.argThat(entry ->
                created.getId().equals(entry.reservationId())
                        && entry.fromStatus() == null // sin estado anterior
                        && entry.toStatus() == ReservationStatus.INITIATED // entra a la maquina
                        && entry.actor() == ActorType.RENTER // el arrendatario inicia
                        && entry.actorId() != null // actor_id = renter
                        && entry.eventIds() != null && entry.eventIds().size() == 1)); // evento info.provided registrado
    }

    @Test
    void transitionAuditCarriesActorIdAndEventIds() { // T-10: cada transicion audita actor_id y event_ids
        Reservation created = createReservation();
        UUID renterActor = UUID.randomUUID();
        clearInvocations(statusAuditPort, outboxPort);

        ChangeReservationStatusResult result = changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                renterActor, null, null, null, null, null));

        verify(statusAuditPort).record(org.mockito.ArgumentMatchers.argThat(entry ->
                renterActor.equals(entry.actorId()) // actor_id registrado
                        && entry.eventIds().size() == 1 // un evento AMQP encolado
                        && result.eventsEnqueued().contains("AMQP"))); // el destino coincide

        // sin eventos encolados -> event_ids vacio
        clearInvocations(statusAuditPort, outboxPort);
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.EXPIRED, null, ActorType.SYSTEM, "TTL expirado"));

        verify(statusAuditPort).record(org.mockito.ArgumentMatchers.argThat(entry ->
                entry.actorId() == null // SYSTEM sin usuario
                        && entry.eventIds().isEmpty())); // ningun evento para EXPIRED
    }

    @Test
    void enqueuesReservedToModuleOneOnPaymentConfirmed() { // T-08: RESERVED notifica a M1 y activa m1_blocked
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        clearInvocations(outboxPort);

        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);

        ArgumentCaptor<OperationalStatusEvent> captor = ArgumentCaptor.forClass(OperationalStatusEvent.class);
        verify(outboxPort).enqueue(captor.capture()); // unico evento M1
        assertThat(captor.getValue().operationalStatus()).isEqualTo(OperationalStatus.RESERVED); // estado ya mapeado
        assertThat(captor.getValue().vesselId()).isEqualTo(created.getVesselId());
        assertThat(captor.getValue().eventId()).isNotNull(); // id unico por transicion (FR-004)
        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.isM1Blocked()).isTrue(); // guarda SC-003 activada
    }

    @Test
    void initiatedAndPendingPaymentDoNotEnqueueModuleOne() { // T-08: ni creacion ni PENDING_PAYMENT notifican a M1
        clearInvocations(outboxPort);
        Reservation created = createReservation(); // creacion en INITIATED

        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);

        verify(outboxPort, never()).enqueue(any(OperationalStatusEvent.class)); // cero eventos a M1
        verify(outboxPort).enqueue(any(StatusChangedEvent.class)); // pero si el evento a M3 (D-03)
        Reservation updated = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(updated.isM1Blocked()).isFalse(); // sin bloqueo notificado
    }

    @Test
    void cancelledBeforeReservedDoesNotEnqueueModuleOne() { // T-08: cancelar antes de RESERVED no emite liberacion a M1
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        clearInvocations(outboxPort, statusAuditPort);

        // cancelacion activa desde PENDING_PAYMENT rechazada: la liberacion indebida jamas se encola
        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, ActorType.RENTER))
                .isInstanceOf(InvalidTransitionException.class);

        verify(outboxPort, never()).enqueue(any(OperationalStatusEvent.class)); // nada a M1
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class)); // nada a M3 tampoco
        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.isM1Blocked()).isFalse();
    }

    @Test
    void cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance() { // T-08: averia del Propietario -> MAINTENANCE_CLEANING
        Reservation created = createReservation();
        changeStatus(created, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER);
        changeStatus(created, ReservationStatus.RESERVED, null, ActorType.SYSTEM);
        clearInvocations(outboxPort);

        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER, ActorType.OWNER, "Averia del propietario",
                UUID.randomUUID(), null, true, null, null, null)); // mechanical_failure = true

        ArgumentCaptor<OperationalStatusEvent> captor = ArgumentCaptor.forClass(OperationalStatusEvent.class);
        verify(outboxPort).enqueue(captor.capture());
        assertThat(captor.getValue().operationalStatus()).isEqualTo(OperationalStatus.MAINTENANCE_CLEANING); // nombre real del spec
    }

    @Test
    void failedTransitionEnqueuesNothing() { // T-08: transicion fallida no encola a ningun destino
        Reservation created = createReservation();
        clearInvocations(outboxPort, statusAuditPort);

        assertThatThrownBy(() -> changeStatus(created, ReservationStatus.COMPLETED, null, ActorType.OWNER))
                .isInstanceOf(InvalidTransitionException.class);

        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
        verify(outboxPort, never()).enqueue(any(OperationalStatusEvent.class));
        verify(statusAuditPort, never()).record(any());
    }

    @Test
    void rejectsPaymentTokenRefWhenTargetIsNotPendingPayment() { // token de pago solo se permite hacia PENDING_PAYMENT
        Reservation created = createReservation();

        assertThatThrownBy(() -> changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                created.getId(), ReservationStatus.EXPIRED, null, ActorType.SYSTEM, "TTL expirado",
                null, null, null, null, null, null, "tok_x")))
                .isInstanceOf(ReservationRuleException.class)
                .hasMessageContaining("payment_token_ref"); // rechaza el token fuera de PENDING_PAYMENT

        Reservation unchanged = reservationRepositoryPort.findById(created.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.INITIATED); // sin cambios persistidos
    }
}
