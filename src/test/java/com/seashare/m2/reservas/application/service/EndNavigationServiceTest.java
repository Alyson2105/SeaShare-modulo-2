package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.EndNavigationCommand;
import com.seashare.m2.reservas.application.dto.EndNavigationResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.CheckoutEventPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeCommand;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeUseCase;
import com.seashare.m2.shared.contracts.dispute.OpenedDispute;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndNavigationServiceTest { // CU-07: reglas de aplicacion del fin de navegacion

    private static final UUID RESERVATION_ID = UUID.randomUUID(); // reserva de prueba

    private static final UUID RENTER_ID = UUID.randomUUID(); // arrendatario titular

    private static final UUID OWNER_ID = UUID.randomUUID(); // propietario titular

    private static final UUID OTHER_OWNER_ID = UUID.randomUUID(); // propietario ajeno

    private static final UUID VESSEL_ID = UUID.randomUUID(); // embarcacion asociada

    private static final Instant NOW = Instant.parse("2027-04-10T19:00:00Z"); // 14:00 en America/Bogota

    private static final Instant ARRIVAL = Instant.parse("2027-04-10T22:30:00Z"); // 17:30 en America/Bogota

    private static final String ZONE = "America/Bogota"; // zona valida

    @Mock
    private ReservationRepositoryPort reservationRepository; // puerto de reservas

    @Mock
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // motor unico de estado

    @Mock
    private CheckoutEventPort checkoutEventPort; // persistencia del evento

    @Mock
    private ClockPort clockPort; // reloj inyectable

    @Mock
    private OpenDisputeUseCase openDisputeUseCase; // contrato compartido de disputa

    private EndNavigationService service; // servicio bajo prueba

    @BeforeEach
    void setUp() { // construye el servicio con la matriz real
        service = new EndNavigationService(reservationRepository, changeReservationStatusUseCase,
                checkoutEventPort, clockPort, new ModuleNotificationMatrix(), openDisputeUseCase);
    }

    private Reservation reservation(ReservationStatus status, String timezone, boolean m1Blocked) { // builder base
        Reservation reservation = Reservation.builder()
                .id(RESERVATION_ID)
                .renterId(RENTER_ID)
                .ownerId(OWNER_ID)
                .vesselId(VESSEL_ID)
                .startDate(LocalDate.of(2027, 4, 10))
                .endDate(LocalDate.of(2027, 4, 12))
                .passengers(2)
                .holderName("Ana")
                .holderPhone("300")
                .holderEmail("ana@example.com")
                .departureTime(LocalTime.of(14, 0))
                .harborTimezone(timezone)
                .ttlExpiresAt(NOW.plusSeconds(600))
                .createdAt(NOW)
                .status(status)
                .version(0L)
                .build();
        reservation.setM1Blocked(m1Blocked);
        return reservation;
    }

    private EndNavigationCommand command(OffsetDateTime actualArrivalAt, String remarks) { // comando del propietario
        return new EndNavigationCommand(RESERVATION_ID, OWNER_ID, ActorType.OWNER, actualArrivalAt, remarks);
    }

    private EndNavigationCommand command(ActorType actor, UUID callerId, String remarks) { // comando con actor variable
        return new EndNavigationCommand(RESERVATION_ID, callerId, actor, null, remarks);
    }

    private ChangeReservationStatusResult statusResult() { // resultado simulado de CU-08
        return new ChangeReservationStatusResult(RESERVATION_ID, ReservationStatus.COMPLETED, null,
                NOW, NOW.plusSeconds(600), 2L, List.of("AMQP", "M1_HTTP"));
    }

    private void givenInNavigation(boolean m1Blocked) { // prepara reserva y CU-08 exitoso
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, m1Blocked)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenReturn(statusResult());
    }

    @Test
    void withoutBodyUsesClockAndBuildsExpectedCommand() { // sin body usa el reloj y el comando completo
        givenInNavigation(true);

        EndNavigationResult result = service.execute(command(null, null));

        ArgumentCaptor<Reservation> savedCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getActualArrivalAt()).isEqualTo(NOW);

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        ChangeReservationStatusCommand captured = commandCaptor.getValue();
        assertThat(captured.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(captured.targetStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(captured.subtype()).isNull();
        assertThat(captured.actor()).isEqualTo(ActorType.OWNER);
        assertThat(captured.reason()).isNull();
        assertThat(captured.actorId()).isEqualTo(OWNER_ID);
        assertThat(captured.expectedVersion()).isNull();
        assertThat(captured.mechanicalFailure()).isFalse();
        assertThat(captured.anticipationMinutes()).isNull();
        assertThat(captured.completionNotes()).isNull();
        assertThat(captured.paymentReference()).isNull();
        assertThat(captured.paymentTokenRef()).isNull();

        verify(checkoutEventPort).save(RESERVATION_ID, OWNER_ID, NOW, null);
        assertThat(result.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(result.actualArrivalAt().toInstant()).isEqualTo(NOW);
        assertThat(result.actualArrivalAt().getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(result.message()).contains("exitosamente").contains("senal");
    }

    @Test
    void remarksArePassedAsCompletionNotesAndChangeMessage() { // novedades llegan a CU-08 y al mensaje
        givenInNavigation(true);
        String remarks = "Motor revisado";

        EndNavigationResult result = service.execute(command(null, remarks));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().completionNotes()).isEqualTo(remarks);
        verify(checkoutEventPort).save(RESERVATION_ID, OWNER_ID, NOW, remarks);
        assertThat(result.message()).contains("con novedades");
    }

    @Test
    void actualArrivalAtOverridesClock() { // la llegada informada manda sobre el reloj
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)));
        when(changeReservationStatusUseCase.execute(any())).thenReturn(statusResult());
        OffsetDateTime informedArrival = OffsetDateTime.parse("2027-04-10T17:30:00-05:00");

        EndNavigationResult result = service.execute(command(informedArrival, null));

        ArgumentCaptor<Reservation> savedCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getActualArrivalAt()).isEqualTo(ARRIVAL);
        assertThat(result.actualArrivalAt()).isEqualTo(informedArrival);
        verify(clockPort, never()).now();
    }

    @Test
    void writesReservationThenCallsCu08ThenInsertsCheckout() { // orden exacto de escrituras
        givenInNavigation(true);

        service.execute(command(null, null));

        InOrder inOrder = inOrder(reservationRepository, changeReservationStatusUseCase, checkoutEventPort);
        inOrder.verify(reservationRepository).save(any());
        inOrder.verify(changeReservationStatusUseCase).execute(any());
        inOrder.verify(checkoutEventPort).save(eq(RESERVATION_ID), eq(OWNER_ID), eq(NOW), isNull());
    }

    @Test
    void unknownReservationIsNotFoundWithoutWrites() { // reserva inexistente
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOf(ReservationNotFoundException.class);
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void renterRoleIsRejectedWithoutWrites() { // rol distinto de OWNER
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(ActorType.RENTER, RENTER_ID, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void ownerOfAnotherReservationIsRejectedWithoutWrites() { // propietario ajeno
        Reservation reservation = reservation(ReservationStatus.IN_NAVIGATION, ZONE, true);
        reservation = Reservation.builder()
                .id(reservation.getId()).renterId(reservation.getRenterId()).ownerId(OTHER_OWNER_ID)
                .vesselId(reservation.getVesselId()).status(reservation.getStatus()).version(0L)
                .startDate(reservation.getStartDate()).endDate(reservation.getEndDate())
                .passengers(reservation.getPassengers()).holderName(reservation.getHolderName())
                .departureTime(reservation.getDepartureTime()).harborTimezone(reservation.getHarborTimezone())
                .ttlExpiresAt(reservation.getTtlExpiresAt()).createdAt(reservation.getCreatedAt()).build();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void remarksOf2000AreAccepted() { // limite exacto valido
        givenInNavigation(true);

        service.execute(command(null, "a".repeat(2000)));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().completionNotes()).hasSize(2000);
    }

    @Test
    void remarksOf2001AreRejectedWithoutWrites() { // limite excedido
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, "a".repeat(2001))))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void blankRemarksBecomeNull() { // solo espacios equivale a sin novedades
        givenInNavigation(true);

        EndNavigationResult result = service.execute(command(null, "   "));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().completionNotes()).isNull();
        verify(checkoutEventPort).save(RESERVATION_ID, OWNER_ID, NOW, null);
        assertThat(result.message()).doesNotContain("novedades");
    }

    @Test
    void completedReservationReturnsTripAlreadyCompletedWithoutWrites() { // cierre repetido
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.COMPLETED, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TRIP_ALREADY_COMPLETED.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void completedReservationWithLongRemarksReturnsInvalidParameters() { // precedencia de 400 sobre 409
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.COMPLETED, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, "a".repeat(2001))))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void reservedReservationIsInvalidStateWithoutWrites() { // estado distinto de IN_NAVIGATION
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void cancelledReservationIsInvalidStateWithoutWrites() { // otro estado invalido
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.CANCELLED, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void missingBlankAndInvalidTimezoneAreUnavailableWithoutWrites() { // zona ausente, blanca o invalida
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(
                Optional.of(reservation(ReservationStatus.IN_NAVIGATION, null, true)),
                Optional.of(reservation(ReservationStatus.IN_NAVIGATION, "   ", true)),
                Optional.of(reservation(ReservationStatus.IN_NAVIGATION, "Mars/Phobos", true)));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void cu08FailurePropagatesAndDoesNotInsertCheckout() { // si CU-08 falla no hay evento
        ReservationRuleException failure = new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "CU-08 failure");
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.execute(command(null, null))).isSameAs(failure);
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void blockedReservationNotifiesMaintenanceAndAddsM1Clause() { // reserva bloqueada en M1
        givenInNavigation(true);

        EndNavigationResult result = service.execute(command(null, null));

        assertThat(result.vesselOperationalStatus()).isEqualTo(OperationalStatus.MAINTENANCE_CLEANING);
        assertThat(result.message()).contains("senal").contains("Mantenimiento/Limpieza");
    }

    @Test
    void unblockedReservationHasNoM1StatusAndOmitsM1Clause() { // reserva sin bloqueo previo
        givenInNavigation(false);

        EndNavigationResult result = service.execute(command(null, null));

        assertThat(result.vesselOperationalStatus()).isNull();
        assertThat(result.message()).doesNotContain("senal").contains("ha pasado a Completada.");
    }

    @Test
    void disputeFieldsAreAlwaysNull() { // compatibilidad: sin disputa configurada no se inventan datos
        givenInNavigation(true);

        EndNavigationResult result = service.execute(command(null, null));

        assertThat(result.disputeId()).isNull();
        assertThat(result.disputeWindowExpiresAt()).isNull();
    }

    @Test
    void completedCheckoutOpensDisputeWithStatusChangedAtAndOwner() { // CU-07 -> CU-16
        givenInNavigation(true);
        UUID disputeId = UUID.randomUUID();
        Instant windowEnd = NOW.plusSeconds(24 * 60 * 60);
        when(openDisputeUseCase.open(any())).thenReturn(
                new OpenedDispute(disputeId, RESERVATION_ID, "PENDING", NOW, windowEnd));

        EndNavigationResult result = service.execute(command(null, null));

        assertThat(result.disputeId()).isEqualTo(disputeId);
        assertThat(result.disputeWindowExpiresAt().toInstant()).isEqualTo(windowEnd);
        ArgumentCaptor<OpenDisputeCommand> captor = ArgumentCaptor.forClass(OpenDisputeCommand.class);
        verify(openDisputeUseCase).open(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new OpenDisputeCommand(RESERVATION_ID, OWNER_ID, NOW));
    }

    @Test
    void retryAfterCompletedDoesNotOpenAnotherDispute() { // reintento no duplica la disputa
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)),
                        Optional.of(reservation(ReservationStatus.COMPLETED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenReturn(statusResult());
        when(openDisputeUseCase.open(any())).thenReturn(
                new OpenedDispute(UUID.randomUUID(), RESERVATION_ID, "PENDING", NOW, NOW.plusSeconds(86400)));

        service.execute(command(null, null));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TRIP_ALREADY_COMPLETED.getCode()));
        verify(openDisputeUseCase, times(1)).open(any());
    }

    @Test
    void disputeFailurePropagatesAndSkipsCheckout() { // el fallo de disputa revierte el cierre
        givenInNavigation(true);
        when(openDisputeUseCase.open(any())).thenThrow(new IllegalStateException("dispute failure"));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dispute failure");
        verify(checkoutEventPort, never()).save(any(), any(), any(), any());
    }
}