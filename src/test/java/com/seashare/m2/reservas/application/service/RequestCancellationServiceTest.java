package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CancellationEventRecord;
import com.seashare.m2.reservas.application.dto.CancellationResult;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.RequestCancellationCommand;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.CancellationEventPort;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestCancellationServiceTest { // CU-04: reglas de aplicacion de la cancelacion

    private static final UUID RESERVATION_ID = UUID.randomUUID(); // reserva de prueba

    private static final UUID RENTER_ID = UUID.randomUUID(); // arrendatario titular

    private static final UUID OWNER_ID = UUID.randomUUID(); // propietario titular

    private static final UUID VESSEL_ID = UUID.randomUUID(); // embarcacion asociada

    private static final Instant ZARPE = Instant.parse("2027-04-10T19:00:00Z"); // 14:00 en America/Bogota

    private static final Instant NOW = ZARPE.minus(Duration.ofHours(72)); // 72 horas exactas antes del zarpe

    @Mock
    private ReservationRepositoryPort reservationRepository; // puerto de reservas

    @Mock
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // motor unico de estado

    @Mock
    private CancellationEventPort cancellationEventPort; // persistencia del evento

    @Mock
    private ClockPort clockPort; // reloj inyectable

    private RequestCancellationService service; // servicio bajo prueba

    @BeforeEach
    void setUp() { // construye el servicio con la matriz real
        service = new RequestCancellationService(reservationRepository, changeReservationStatusUseCase,
                cancellationEventPort, clockPort, new ModuleNotificationMatrix());
    }

    private Reservation.Builder reservationBuilder(ReservationStatus status) { // builder base de reserva
        return Reservation.builder()
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
                .estimatedTotal(new BigDecimal("100.00"))
                .departureTime(LocalTime.of(14, 0))
                .harborTimezone("America/Bogota")
                .ttlExpiresAt(NOW.plusSeconds(600))
                .createdAt(NOW)
                .status(status)
                .version(0L);
    }

    private Reservation reserved(boolean m1Blocked) { // reserva RESERVED con guarda M1 controlada
        Reservation reservation = reservationBuilder(ReservationStatus.RESERVED).build();
        reservation.setM1Blocked(m1Blocked);
        return reservation;
    }

    private RequestCancellationCommand renterCommand(String justification) { // comando del arrendatario
        return new RequestCancellationCommand(RESERVATION_ID, RENTER_ID, ActorType.RENTER, "ignored", justification);
    }

    private RequestCancellationCommand ownerCommand(String reason, String justification) { // comando del propietario
        return new RequestCancellationCommand(RESERVATION_ID, OWNER_ID, ActorType.OWNER, reason, justification);
    }

    private ChangeReservationStatusResult statusResult(CancellationSubtype subtype, List<String> events) { // resultado simulado de CU-08
        return new ChangeReservationStatusResult(RESERVATION_ID, ReservationStatus.CANCELLED, subtype,
                NOW, NOW.plusSeconds(600), 1L, events);
    }

    @Test
    void renterWith72HoursCancelsFlexible() { // borde flexible y contrato completo
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.FLEXIBLE, List.of("AMQP", "M1_HTTP")));

        CancellationResult result = service.execute(renterCommand("Renter justification"));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        ChangeReservationStatusCommand command = commandCaptor.getValue();
        assertThat(command.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(command.targetStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(command.subtype()).isEqualTo(CancellationSubtype.FLEXIBLE);
        assertThat(command.actor()).isEqualTo(ActorType.RENTER);
        assertThat(command.reason()).isEqualTo("Renter justification");
        assertThat(command.actorId()).isEqualTo(RENTER_ID);
        assertThat(command.expectedVersion()).isNull();
        assertThat(command.mechanicalFailure()).isFalse();
        assertThat(command.anticipationMinutes()).isEqualTo(4320);
        assertThat(command.completionNotes()).isNull();
        assertThat(command.paymentReference()).isNull();
        assertThat(command.paymentTokenRef()).isNull();

        ArgumentCaptor<CancellationEventRecord> eventCaptor = ArgumentCaptor.forClass(CancellationEventRecord.class);
        verify(cancellationEventPort).save(eventCaptor.capture());
        CancellationEventRecord event = eventCaptor.getValue();
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.actor()).isEqualTo(ActorType.RENTER);
        assertThat(event.requestedAt()).isEqualTo(NOW);
        assertThat(event.anticipationMinutes()).isEqualTo(4320);
        assertThat(event.cancellationSubtype()).isEqualTo(CancellationSubtype.FLEXIBLE);
        assertThat(event.reason()).isNull();
        assertThat(event.justification()).isEqualTo("Renter justification");

        assertThat(result.anticipationHours()).isEqualTo(72.0);
        assertThat(result.portTimezone()).isEqualTo("America/Bogota");
        assertThat(result.scheduledDeparture().toInstant()).isEqualTo(ZARPE);
        assertThat(result.scheduledDeparture().getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(result.cancelledAt().toInstant()).isEqualTo(NOW);
        assertThat(result.m1SyncStatus()).isEqualTo("ENQUEUED");
        assertThat(result.m3NotificationStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void renterWith30HoursCancelsModerate() { // franja moderada
        Instant now = ZARPE.minus(Duration.ofHours(30));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(now);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.MODERATE, List.of("AMQP", "M1_HTTP")));

        CancellationResult result = service.execute(renterCommand(null));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().subtype()).isEqualTo(CancellationSubtype.MODERATE);
        assertThat(commandCaptor.getValue().anticipationMinutes()).isEqualTo(1800);
        assertThat(result.anticipationHours()).isEqualTo(30.0);
    }

    @Test
    void renterWith10HoursCancelsLate() { // franja tardia
        Instant now = ZARPE.minus(Duration.ofHours(10));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(now);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.LATE, List.of("AMQP", "M1_HTTP")));

        CancellationResult result = service.execute(renterCommand(null));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().subtype()).isEqualTo(CancellationSubtype.LATE);
        assertThat(commandCaptor.getValue().anticipationMinutes()).isEqualTo(600);
        assertThat(result.anticipationHours()).isEqualTo(10.0);
    }

    @Test
    void ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance() { // averia con aviso a M1
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.BY_OWNER, List.of("AMQP", "M1_HTTP")));

        CancellationResult result = service.execute(ownerCommand("mechanical_failure", "Motor averiado"));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        ChangeReservationStatusCommand command = commandCaptor.getValue();
        assertThat(command.subtype()).isEqualTo(CancellationSubtype.BY_OWNER);
        assertThat(command.mechanicalFailure()).isTrue();
        assertThat(command.anticipationMinutes()).isNull();
        assertThat(command.reason()).isEqualTo("mechanical_failure");
        assertThat(command.actorId()).isEqualTo(OWNER_ID);

        ArgumentCaptor<CancellationEventRecord> eventCaptor = ArgumentCaptor.forClass(CancellationEventRecord.class);
        verify(cancellationEventPort).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().anticipationMinutes()).isZero();
        assertThat(eventCaptor.getValue().reason()).isEqualTo("mechanical_failure");
        assertThat(eventCaptor.getValue().cancellationSubtype()).isEqualTo(CancellationSubtype.BY_OWNER);

        assertThat(result.vesselNewOperationalStatus()).isEqualTo(OperationalStatus.MAINTENANCE_CLEANING);
        assertThat(result.anticipationHours()).isNull();
        assertThat(result.m1SyncStatus()).isEqualTo("ENQUEUED");
        assertThat(result.m3NotificationStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable() { // fuerza mayor libera la embarcacion
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.BY_OWNER, List.of("AMQP", "M1_HTTP")));

        CancellationResult result = service.execute(ownerCommand("logistics_force_majeure", "Puerto cerrado"));

        ArgumentCaptor<ChangeReservationStatusCommand> commandCaptor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatusUseCase).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().mechanicalFailure()).isFalse();
        assertThat(result.vesselNewOperationalStatus()).isEqualTo(OperationalStatus.AVAILABLE);
        assertThat(result.m1SyncStatus()).isEqualTo("ENQUEUED");
        assertThat(result.m3NotificationStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void ownerWithoutM1BlockDoesNotNotifyModuleOne() { // sin bloqueo previo no hay aviso M1
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(false)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.BY_OWNER, List.of("AMQP")));

        CancellationResult result = service.execute(ownerCommand("logistics_force_majeure", null));

        assertThat(result.vesselNewOperationalStatus()).isNull();
        assertThat(result.m1SyncStatus()).isNull();
        assertThat(result.m3NotificationStatus()).isEqualTo("PUBLISHED");
        verify(cancellationEventPort).save(any());
    }

    @Test
    void ownerWithoutReasonIsRejected() { // motivo obligatorio para propietario
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));

        assertThatThrownBy(() -> service.execute(ownerCommand(null, null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(cancellationEventPort, never()).save(any());
        verify(clockPort, never()).now();
    }

    @Test
    void ownerWithInvalidReasonIsRejected() { // motivo no soportado
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));

        assertThatThrownBy(() -> service.execute(ownerCommand("other_reason", null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(cancellationEventPort, never()).save(any());
    }

    @Test
    void foreignRenterIsRejected() { // arrendatario ajeno
        Reservation reservation = reservationBuilder(ReservationStatus.RESERVED).renterId(UUID.randomUUID()).build();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.RESERVATION_NOT_OWNED.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
    }

    @Test
    void foreignOwnerIsRejected() { // propietario ajeno
        Reservation reservation = reservationBuilder(ReservationStatus.RESERVED).ownerId(UUID.randomUUID()).build();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.execute(ownerCommand("logistics_force_majeure", null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.RESERVATION_NOT_OWNED.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
    }

    @Test
    void unknownReservationIsNotFound() { // reserva inexistente
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOf(ReservationNotFoundException.class);
        verify(clockPort, never()).now();
    }

    @Test
    void renterAtDepartureIsRejected() { // zarpe exacto
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(ZARPE);

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(cancellationEventPort, never()).save(any());
    }

    @Test
    void renterAfterDepartureIsRejected() { // zarpe superado
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(ZARPE.plusSeconds(1));

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
    }

    @Test
    void ownerCanCancelAfterDeparture() { // el propietario no mide tiempo
        Instant afterDeparture = ZARPE.plus(Duration.ofHours(1));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(afterDeparture);
        when(changeReservationStatusUseCase.execute(any()))
                .thenReturn(statusResult(CancellationSubtype.BY_OWNER, List.of("AMQP", "M1_HTTP")));

        CancellationResult result = service.execute(ownerCommand("logistics_force_majeure", null));

        assertThat(result.cancelledAt().toInstant()).isEqualTo(afterDeparture);
        verify(cancellationEventPort).save(any());
    }

    @Test
    void reservationNotReservedIsRejected() { // estado distinto de RESERVED
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservationBuilder(ReservationStatus.PENDING_PAYMENT).build()));

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(clockPort, never()).now();
    }

    @Test
    void nullHarborTimezoneIsUnavailable() { // zona ausente
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservationBuilder(ReservationStatus.RESERVED).harborTimezone(null).build()));

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        verify(clockPort, never()).now();
    }

    @Test
    void invalidHarborTimezoneIsUnavailable() { // zona no interpretable
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservationBuilder(ReservationStatus.RESERVED)
                        .harborTimezone("Mars/Phobos").build()));

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode()));
        verify(clockPort, never()).now();
    }

    @Test
    void nullDepartureTimeIsInternalError() { // dato de zarpe incompleto
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservationBuilder(ReservationStatus.RESERVED).departureTime(null).build()));

        assertThatThrownBy(() -> service.execute(renterCommand(null)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode()));
        verify(clockPort, never()).now();
    }

    @Test
    void justificationLongerThan500IsRejected() { // limite del texto libre
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));

        assertThatThrownBy(() -> service.execute(renterCommand("a".repeat(501))))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(changeReservationStatusUseCase, never()).execute(any());
        verify(clockPort, never()).now();
    }

    @Test
    void changeStatusFailurePropagatesAndDoesNotSaveEvent() { // si CU-08 falla no hay evento
        ReservationRuleException failure = new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "CU-08 failure");
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reserved(true)));
        when(clockPort.now()).thenReturn(NOW);
        when(changeReservationStatusUseCase.execute(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.execute(renterCommand(null))).isSameAs(failure);
        verify(cancellationEventPort, never()).save(any());
    }
}