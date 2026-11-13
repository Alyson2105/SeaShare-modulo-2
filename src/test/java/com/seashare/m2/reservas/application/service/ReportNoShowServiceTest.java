package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.ReportNoShowCommand;
import com.seashare.m2.reservas.application.dto.ReportNoShowResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.NoShowEventPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportNoShowServiceTest { // CU-05: reglas de aplicacion de la inasistencia

    private static final UUID RESERVATION_ID = UUID.randomUUID(); // reserva de prueba

    private static final UUID RENTER_ID = UUID.randomUUID(); // arrendatario titular

    private static final UUID OWNER_ID = UUID.randomUUID(); // propietario titular

    private static final UUID OTHER_OWNER_ID = UUID.randomUUID(); // propietario ajeno

    private static final UUID VESSEL_ID = UUID.randomUUID(); // embarcacion asociada

    private static final Instant SCHEDULED = Instant.parse("2027-04-10T19:00:00Z"); // 14:00 en America/Bogota

    private static final Instant NOW = SCHEDULED.plusSeconds(30 * 60); // exactamente 30 minutos despues

    private static final String ZONE = "America/Bogota"; // zona valida

    @Mock
    private ReservationRepositoryPort reservationRepository; // puerto de reservas

    @Mock
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // motor unico de estado

    @Mock
    private NoShowEventPort noShowEventPort; // persistencia del evento

    @Mock
    private ClockPort clockPort; // reloj inyectable

    private ReportNoShowService service; // servicio bajo prueba

    @BeforeEach
    void setUp() { // construye el servicio con la matriz real
        service = new ReportNoShowService(reservationRepository, changeReservationStatusUseCase,
                noShowEventPort, clockPort, new ModuleNotificationMatrix());
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

    private ReportNoShowCommand command(ActorType actor, UUID callerId, String observations) { // comando configurable
        return new ReportNoShowCommand(RESERVATION_ID, callerId, actor, observations);
    }

    private ReportNoShowCommand command(String observations) { // comando del propietario
        return command(ActorType.OWNER, OWNER_ID, observations);
    }

    private ChangeReservationStatusResult statusResult(Instant at) { // resultado simulado de CU-08
        return new ChangeReservationStatusResult(RESERVATION_ID, ReservationStatus.CANCELLED,
                CancellationSubtype.NO_SHOW, at, at.plusSeconds(600), 2L, List.of("AMQP", "M1_HTTP"));
    }

    private void givenReservation(ReservationStatus status, String timezone, boolean m1Blocked) { // solo lectura
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(status, timezone, m1Blocked)));
    }

    private void givenReserved(Instant at, boolean m1Blocked) { // reserva, reloj y CU-08 exitoso
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, ZONE, m1Blocked)));
        when(clockPort.now()).thenReturn(at);
        when(changeReservationStatusUseCase.execute(any())).thenReturn(statusResult(at));
    }

    private void givenReserved() { // caso exitoso con M1 bloqueado
        givenReserved(NOW, true);
    }

    @Test
    void exactlyThirtyMinutesIsAcceptedWithExpectedCommand() { // 30 minutos exactos y comando completo
        givenReserved();

        ReportNoShowResult result = service.execute(command("Llego tarde"));

        ArgumentCaptor<ChangeReservationStatusCommand> captor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(captor.capture());
        ChangeReservationStatusCommand captured = captor.getValue();
        assertThat(captured.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(captured.targetStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(captured.subtype()).isEqualTo(CancellationSubtype.NO_SHOW);
        assertThat(captured.actor()).isEqualTo(ActorType.OWNER);
        assertThat(captured.reason()).isNull();
        assertThat(captured.actorId()).isEqualTo(OWNER_ID);
        assertThat(captured.expectedVersion()).isNull();
        assertThat(captured.mechanicalFailure()).isFalse();
        assertThat(captured.anticipationMinutes()).isNull();
        assertThat(captured.completionNotes()).isNull();
        assertThat(captured.paymentReference()).isNull();
        assertThat(captured.paymentTokenRef()).isNull();

        verify(noShowEventPort).save(RESERVATION_ID, OWNER_ID, NOW, 30, "Llego tarde");
        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.cancellationSubtype()).isEqualTo(CancellationSubtype.NO_SHOW);
        assertThat(result.waitMinutesRecorded()).isEqualTo(30);
        assertThat(result.cancelledAt().toInstant()).isEqualTo(NOW);
        assertThat(result.cancelledAt().getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(result.vesselId()).isEqualTo(VESSEL_ID);
        assertThat(result.vesselOperationalStatus()).isEqualTo(OperationalStatus.AVAILABLE);
        assertThat(result.message()).contains("Disponible");
    }

    @Test
    void thirtyFiveMinutesFortySecondsRecords35Minutes() { // minutos completos hacia abajo
        givenReserved(NOW.plusSeconds(5 * 60 + 40), true);

        ReportNoShowResult result = service.execute(command(null));

        assertThat(result.waitMinutesRecorded()).isEqualTo(35);
        verify(noShowEventPort).save(RESERVATION_ID, OWNER_ID, NOW.plusSeconds(5 * 60 + 40), 35, null);
    }

    @Test
    void cu08RunsBeforeNoShowEventInsert() { // orden exacto de escrituras
        givenReserved();

        service.execute(command("Chaleco mojado"));

        InOrder inOrder = inOrder(changeReservationStatusUseCase, noShowEventPort);
        inOrder.verify(changeReservationStatusUseCase).execute(any());
        inOrder.verify(noShowEventPort).save(RESERVATION_ID, OWNER_ID, NOW, 30, "Chaleco mojado");
    }

    @Test
    void unknownReservationIsNotFoundWithoutWrites() { // reserva inexistente
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(null)))
                .isInstanceOf(ReservationNotFoundException.class);
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void renterRoleIsRejectedWithoutWrites() { // rol distinto de OWNER
        givenReservation(ReservationStatus.RESERVED, ZONE, true);

        assertThatThrownBy(() -> service.execute(command(ActorType.RENTER, RENTER_ID, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void foreignOwnerIsRejectedWithoutWrites() { // propietario ajeno
        givenReservation(ReservationStatus.RESERVED, ZONE, true);

        assertThatThrownBy(() -> service.execute(command(ActorType.OWNER, OTHER_OWNER_ID, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void observationsOf500AreAccepted() { // limite exacto valido
        givenReserved();
        String observations = "a".repeat(500);

        service.execute(command(observations));

        verify(noShowEventPort).save(RESERVATION_ID, OWNER_ID, NOW, 30, observations);
    }

    @Test
    void observationsOf501AreRejectedWithoutWrites() { // limite excedido
        givenReservation(ReservationStatus.RESERVED, ZONE, true);

        assertThatThrownBy(() -> service.execute(command("a".repeat(501))))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void blankObservationsBecomeNull() { // solo espacios equivale a sin observaciones
        givenReserved();

        service.execute(command("   "));

        verify(noShowEventPort).save(RESERVATION_ID, OWNER_ID, NOW, 30, null);
    }

    @Test
    void invalidStatesAreRejectedWithoutClockOrWrites() { // estados distintos de RESERVED
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(
                Optional.of(reservation(ReservationStatus.IN_NAVIGATION, ZONE, true)),
                Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, ZONE, true)),
                Optional.of(reservation(ReservationStatus.CANCELLED, ZONE, true)),
                Optional.of(reservation(ReservationStatus.COMPLETED, ZONE, true)));

        for (int i = 0; i < 4; i++) { // todos comparten el mismo error
            assertThatThrownBy(() -> service.execute(command(null)))
                    .isInstanceOfSatisfying(ReservationRuleException.class,
                            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        }
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void missingBlankAndInvalidTimezoneAreUnavailableWithoutWrites() { // zona ausente, blanca o invalida
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(
                Optional.of(reservation(ReservationStatus.RESERVED, null, true)),
                Optional.of(reservation(ReservationStatus.RESERVED, "   ", true)),
                Optional.of(reservation(ReservationStatus.RESERVED, "Mars/Phobos", true)));

        for (int i = 0; i < 3; i++) { // las tres variantes comparten 503
            assertThatThrownBy(() -> service.execute(command(null)))
                    .isInstanceOfSatisfying(ReservationRuleException.class,
                            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        }
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
        verify(clockPort, never()).now();
    }

    @Test
    void oneSecondBeforeScheduledDepartureIsTripNotStarted() { // aun no zarpo
        givenReservation(ReservationStatus.RESERVED, ZONE, true);
        when(clockPort.now()).thenReturn(SCHEDULED.minusSeconds(1));

        assertThatThrownBy(() -> service.execute(command(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.TRIP_NOT_STARTED.getCode());
                    assertThat(e.getMessage()).contains("2027-04-10T14:00:00-05:00");
                });
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
    }

    @Test
    void eighteenMinutesAfterDepartureShowsTwelveMinutesAndZeroSeconds() { // tolerancia vigente exacta
        givenReservation(ReservationStatus.RESERVED, ZONE, true);
        when(clockPort.now()).thenReturn(SCHEDULED.plusSeconds(18 * 60));

        assertThatThrownBy(() -> service.execute(command(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.GRACE_PERIOD_NOT_ELAPSED.getCode());
                    assertThat(e.getMessage()).contains("12 minutes").contains("0 seconds");
                });
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
    }

    @Test
    void seventeenMinutesThirtySecondsAfterDepartureShowsTwelveMinutesAndThirtySeconds() { // redondeo hacia arriba
        givenReservation(ReservationStatus.RESERVED, ZONE, true);
        when(clockPort.now()).thenReturn(SCHEDULED.plusSeconds(17 * 60 + 30));

        assertThatThrownBy(() -> service.execute(command(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.GRACE_PERIOD_NOT_ELAPSED.getCode());
                    assertThat(e.getMessage()).contains("12 minutes").contains("30 seconds");
                });
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
    }

    @Test
    void oneSecondBeforeGraceEndsShowsZeroMinutesAndOneSecond() { // borde de un segundo
        givenReservation(ReservationStatus.RESERVED, ZONE, true);
        when(clockPort.now()).thenReturn(SCHEDULED.plusSeconds(30 * 60 - 1));

        assertThatThrownBy(() -> service.execute(command(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.GRACE_PERIOD_NOT_ELAPSED.getCode());
                    assertThat(e.getMessage()).contains("0 minutes").contains("1 seconds");
                });
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
    }

    @Test
    void cu08FailurePropagatesAndDoesNotInsertNoShow() { // si CU-08 falla no hay evento
        ReservationRuleException failure = new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "CU-08 failure");
        givenReservation(ReservationStatus.RESERVED, ZONE, true);
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.execute(command(null))).isSameAs(failure);
        verify(noShowEventPort, never()).save(any(), any(), any(), anyInt(), any());
    }

    @Test
    void m1NotBlockedLeavesOperationalStatusNullAndMessageWithoutAvailable() { // sin bloqueo previo en M1
        givenReserved(NOW, false);

        ReportNoShowResult result = service.execute(command(null));

        assertThat(result.vesselOperationalStatus()).isNull();
        assertThat(result.message()).doesNotContain("Disponible");
    }

    @Test
    void m1BlockedReleasesVesselAsAvailable() { // con bloqueo previo en M1
        givenReserved(NOW, true);

        ReportNoShowResult result = service.execute(command(null));

        assertThat(result.vesselOperationalStatus()).isEqualTo(OperationalStatus.AVAILABLE);
        assertThat(result.message()).contains("Disponible");
    }
}