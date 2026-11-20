package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.StartNavigationCommand;
import com.seashare.m2.reservas.application.dto.StartNavigationResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.CheckinEventPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartNavigationServiceTest { // CU-06: reglas de aplicacion del inicio de navegacion

    private static final UUID RESERVATION_ID = UUID.randomUUID(); // reserva de prueba

    private static final UUID RENTER_ID = UUID.randomUUID(); // arrendatario titular

    private static final UUID OWNER_ID = UUID.randomUUID(); // propietario titular

    private static final UUID OTHER_OWNER_ID = UUID.randomUUID(); // propietario ajeno

    private static final UUID VESSEL_ID = UUID.randomUUID(); // embarcacion asociada

    private static final Instant NOW = Instant.parse("2027-04-10T19:00:00Z"); // 14:00 en America/Bogota

    private static final String ZONE = "America/Bogota"; // zona valida

    private static final OffsetDateTime INFORMED_DEPARTURE = OffsetDateTime.parse("2027-04-10T15:00:00-05:00"); // salida informada

    @Mock
    private ReservationRepositoryPort reservationRepository; // puerto de reservas

    @Mock
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // motor unico de estado

    @Mock
    private CheckinEventPort checkinEventPort; // persistencia del evento

    @Mock
    private ClockPort clockPort; // reloj inyectable

    private StartNavigationService service; // servicio bajo prueba

    @BeforeEach
    void setUp() { // construye el servicio con la matriz real
        service = new StartNavigationService(reservationRepository, changeReservationStatusUseCase,
                checkinEventPort, clockPort, new ModuleNotificationMatrix());
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
        reservation.setM1Blocked(m1Blocked); // guarda de bloqueo en M1
        return reservation;
    }

    private StartNavigationCommand command(OffsetDateTime actualDepartureAt, String deliveryNotes) { // comando del propietario
        return new StartNavigationCommand(RESERVATION_ID, OWNER_ID, ActorType.OWNER, actualDepartureAt, deliveryNotes);
    }

    private StartNavigationCommand command(ActorType actor, UUID callerId, String deliveryNotes) { // comando con actor variable
        return new StartNavigationCommand(RESERVATION_ID, callerId, actor, null, deliveryNotes);
    }

    private ChangeReservationStatusResult statusResult(List<String> destinations) { // resultado simulado de CU-08
        return new ChangeReservationStatusResult(RESERVATION_ID, ReservationStatus.IN_NAVIGATION, null,
                NOW, NOW.plusSeconds(600), 2L, destinations);
    }

    private void givenReserved() { // prepara reserva RESERVED, reloj y CU-08 exitoso
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(List.of("AMQP", "M1_HTTP")));
    }

    @Test
    void withoutBodyUsesClockAndBuildsExpectedCommand() { // sin body usa el reloj y el comando completo
        givenReserved();

        StartNavigationResult result = service.execute(command(null, null));

        ArgumentCaptor<Reservation> savedCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getActualDepartureAt()).isEqualTo(NOW);

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        ChangeReservationStatusCommand captured = commandCaptor.getValue();
        assertThat(captured.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(captured.targetStatus()).isEqualTo(ReservationStatus.IN_NAVIGATION);
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

        verify(checkinEventPort).save(RESERVATION_ID, OWNER_ID, NOW, null);
        assertThat(result.status()).isEqualTo(ReservationStatus.IN_NAVIGATION);
        assertThat(result.actualDepartureAt().toInstant()).isEqualTo(NOW);
        assertThat(result.actualDepartureAt().getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(result.vesselOperationalStatus()).isEqualTo(OperationalStatus.IN_NAVIGATION);
        assertThat(result.insuranceActivated()).isTrue();
        assertThat(result.message()).contains("Modulo 3").contains("seguro nautico");
    }

    @Test
    void deliveryNotesReachCheckinButNotCu08() { // las notas van a check-in y no a CU-08
        givenReserved();
        String notes = "Chalecos revisados";

        service.execute(command(null, notes));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().completionNotes()).isNull();
        verify(checkinEventPort).save(RESERVATION_ID, OWNER_ID, NOW, notes);
    }

    @Test
    void informedDepartureOverridesClock() { // la salida informada manda sobre el reloj
        givenReserved();

        StartNavigationResult result = service.execute(command(INFORMED_DEPARTURE, null));

        ArgumentCaptor<Reservation> savedCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getActualDepartureAt()).isEqualTo(INFORMED_DEPARTURE.toInstant());
        assertThat(result.actualDepartureAt()).isEqualTo(INFORMED_DEPARTURE);
        verify(checkinEventPort).save(RESERVATION_ID, OWNER_ID, INFORMED_DEPARTURE.toInstant(), null);
    }

    @Test
    void writesVesselQueryThenReservationThenCu08ThenCheckin() { // orden exacto de escrituras
        givenReserved();

        service.execute(command(null, null));

        InOrder inOrder = inOrder(reservationRepository, changeReservationStatusUseCase, checkinEventPort);
        inOrder.verify(reservationRepository).existsOtherInNavigation(VESSEL_ID, RESERVATION_ID);
        inOrder.verify(reservationRepository).save(any());
        inOrder.verify(changeReservationStatusUseCase).execute(any());
        inOrder.verify(checkinEventPort).save(eq(RESERVATION_ID), eq(OWNER_ID), eq(NOW), isNull());
    }

    @Test
    void unknownReservationIsNotFoundWithoutWrites() { // reserva inexistente
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOf(ReservationNotFoundException.class);
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void renterRoleIsRejectedWithoutWrites() { // rol distinto de OWNER
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(ActorType.RENTER, RENTER_ID, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void ownerOfAnotherReservationIsRejectedWithoutWrites() { // propietario ajeno
        Reservation reservation = reservation(ReservationStatus.RESERVED, ZONE, true);
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
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void deliveryNotesOf1000AreAccepted() { // limite exacto valido
        givenReserved();

        service.execute(command(null, "a".repeat(1000)));

        verify(checkinEventPort).save(RESERVATION_ID, OWNER_ID, NOW, "a".repeat(1000));
    }

    @Test
    void deliveryNotesOf1001AreRejectedWithoutWrites() { // limite excedido
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, "a".repeat(1001))))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void blankDeliveryNotesBecomeNull() { // solo espacios equivale a sin notas
        givenReserved();

        service.execute(command(null, "   "));

        verify(checkinEventPort).save(RESERVATION_ID, OWNER_ID, NOW, null);
    }

    @Test
    void inNavigationWithLongNotesReturnsInvalidParametersFirst() { // precedencia de 400 sobre 409
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, "a".repeat(1001))))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void inNavigationReturnsTripAlreadyStartedWithoutWrites() { // inicio repetido
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TRIP_ALREADY_STARTED.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites() { // otros estados invalidos
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(
                Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, ZONE, true)),
                Optional.of(reservation(ReservationStatus.CANCELLED, ZONE, true)),
                Optional.of(reservation(ReservationStatus.COMPLETED, ZONE, true)));

        for (int i = 0; i < 3; i++) { // los tres estados comparten el mismo error
            assertThatThrownBy(() -> service.execute(command(null, null)))
                    .isInstanceOfSatisfying(ReservationRuleException.class,
                            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        }
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void missingBlankAndInvalidTimezoneAreUnavailableWithoutWrites() { // zona ausente, blanca o invalida
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(
                Optional.of(reservation(ReservationStatus.RESERVED, null, true)),
                Optional.of(reservation(ReservationStatus.RESERVED, "   ", true)),
                Optional.of(reservation(ReservationStatus.RESERVED, "Mars/Phobos", true)));

        for (int i = 0; i < 3; i++) { // las tres variantes comparten 503
            assertThatThrownBy(() -> service.execute(command(null, null)))
                    .isInstanceOfSatisfying(ReservationRuleException.class,
                            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        }
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void oneSecondBeforeScheduledDepartureIsEarly() { // salida un segundo antes del zarpe
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW.minusSeconds(1));

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.EARLY_DEPARTURE.getCode());
                    assertThat(e.getMessage()).contains("2027-04-10T14:00:00-05:00");
                });
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void exactScheduledDepartureIsAccepted() { // el segundo exacto del zarpe ya es valido
        givenReserved();

        StartNavigationResult result = service.execute(command(null, null));

        assertThat(result.status()).isEqualTo(ReservationStatus.IN_NAVIGATION);
        verify(checkinEventPort).save(RESERVATION_ID, OWNER_ID, NOW, null);
    }

    @Test
    void informedDepartureBeforeScheduledIsInvalidParameters() { // salida informada anterior al zarpe
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);

        OffsetDateTime beforeScheduled = OffsetDateTime.parse("2027-04-10T13:59:00-05:00");

        assertThatThrownBy(() -> service.execute(command(beforeScheduled, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void anotherReservationInNavigationIsRejectedWithoutWrites() { // barco en navegacion
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.existsOtherInNavigation(VESSEL_ID, RESERVATION_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(command(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VESSEL_CURRENTLY_IN_NAVIGATION.getCode()));
        verify(reservationRepository).existsOtherInNavigation(VESSEL_ID, RESERVATION_ID);
        verify(reservationRepository, never()).save(any());
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void cu08FailurePropagatesAndDoesNotInsertCheckin() { // si CU-08 falla no hay evento
        ReservationRuleException failure = new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "CU-08 failure");
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.execute(command(null, null))).isSameAs(failure);
        verify(checkinEventPort, never()).save(any(), any(), any(), any());
    }

    @Test
    void insuranceIsFalseWhenAmqpDestinationIsMissing() { // sin AMQP no se activa el seguro
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenReturn(statusResult(List.of("M1_HTTP")));

        StartNavigationResult result = service.execute(command(null, null));

        assertThat(result.insuranceActivated()).isFalse();
        assertThat(result.vesselOperationalStatus()).isEqualTo(OperationalStatus.IN_NAVIGATION);
        assertThat(result.message()).doesNotContain("Modulo 3").endsWith("En Navegacion.");
    }

    @Test
    void exactlyThirtyMinutesLateIsStillPunctualAndThirtyOneIsLate() { // umbral estricto de 30 minutos
        givenReserved();
        OffsetDateTime exactTolerance = NOW.plusSeconds(30 * 60).atZone(ZoneOffset.ofHours(-5)).toOffsetDateTime();
        OffsetDateTime overTolerance = NOW.plusSeconds(31 * 60).atZone(ZoneOffset.ofHours(-5)).toOffsetDateTime();

        StartNavigationResult punctual = service.execute(command(exactTolerance, null));
        StartNavigationResult late = service.execute(command(overTolerance, null));

        assertThat(punctual.message()).contains("exitosamente").doesNotContain("llegada tardia");
        assertThat(late.message()).contains("llegada tardia").contains("inasistencia");
    }
}