package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.MaintenanceEventCommand;
import com.seashare.m2.reservas.application.dto.MaintenanceEventOutcome;
import com.seashare.m2.reservas.application.dto.MaintenanceEventResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.CancellationCause;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplyMaintenanceEventServiceTest { // pruebas unitarias de C5

    private static final Instant NOW = Instant.parse("2026-11-19T15:15:00Z");

    private static final String ENDPOINT = ApplyMaintenanceEventService.IDEMPOTENCY_ENDPOINT;

    private static final UUID VESSEL_ID = UUID.fromString("37a31793-29e9-4765-9d3d-39b74a744476");

    private ReservationRepositoryPort repository;

    private ChangeReservationStatusUseCase changeReservationStatus;

    private IdempotencyPort idempotencyPort;

    private ApplyMaintenanceEventService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(ReservationRepositoryPort.class);
        changeReservationStatus = org.mockito.Mockito.mock(ChangeReservationStatusUseCase.class);
        idempotencyPort = org.mockito.Mockito.mock(IdempotencyPort.class);
        ClockPort clock = () -> NOW;
        service = new ApplyMaintenanceEventService(repository, changeReservationStatus, idempotencyPort, clock, true);
        when(idempotencyPort.tryClaimPermanent(any(), any(), any())).thenReturn(true);
        when(repository.existsAnyByVesselId(VESSEL_ID)).thenReturn(true);
    }

    @Test
    void duplicateEventReturnsDuplicateWithoutClaims() { // reenvio del mismo event_id
        when(idempotencyPort.findOutcome("evt-1", ENDPOINT)).thenReturn(Optional.of("APPLIED"));

        MaintenanceEventResult result = service.apply(command("evt-1", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.DUPLICATE);
        verify(idempotencyPort, never()).tryClaimPermanent(any(), any(), any());
        verify(changeReservationStatus, never()).execute(any());
    }

    @Test
    void unknownVesselIsReportedWithoutCancelling() { // M2 no tiene historial local
        when(repository.existsAnyByVesselId(VESSEL_ID)).thenReturn(false);

        MaintenanceEventResult result = service.apply(command("evt-2", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.UNKNOWN_VESSEL);
        verify(idempotencyPort).completeWithOutcome("evt-2", ENDPOINT, "UNKNOWN_VESSEL");
    }

    @Test
    void pastRangeReturnsInvalidRange() { // rango valido integramente en el pasado
        MaintenanceEventResult result = service.apply(command("evt-3", "2026-11-01T08:00:00-05:00", "2026-11-10T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.INVALID_RANGE);
        verify(changeReservationStatus, never()).execute(any());
    }

    @Test
    void autoCancelDisabledReturnsOutcome() { // bandera apagada
        ApplyMaintenanceEventService disabled = new ApplyMaintenanceEventService(
                repository, changeReservationStatus, idempotencyPort, () -> NOW, false);

        MaintenanceEventResult result = disabled.apply(command("evt-4", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.AUTO_CANCEL_DISABLED);
        verify(changeReservationStatus, never()).execute(any());
    }

    @Test
    void noAffectedReservationsWhenNoneCross() { // barco con historial pero sin RESERVED en el rango
        when(repository.findReservedByVesselOverlapping(any(), any(), any())).thenReturn(List.of());

        MaintenanceEventResult result = service.apply(command("evt-5", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.NO_AFFECTED_RESERVATIONS);
        verify(changeReservationStatus, never()).execute(any());
    }

    @Test
    void appliedCancelsEveryCrossingReservation() { // una transaccion por reserva afectada
        Reservation first = reservation(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 26));
        Reservation second = reservation(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 28));
        when(repository.findReservedByVesselOverlapping(any(), any(), any()))
                .thenReturn(List.of(first.getId(), second.getId()));
        when(repository.findById(first.getId())).thenReturn(Optional.of(first));
        when(repository.findById(second.getId())).thenReturn(Optional.of(second));

        MaintenanceEventResult result = service.apply(command("evt-6", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.APPLIED);
        verify(changeReservationStatus, org.mockito.Mockito.times(2)).execute(any(ChangeReservationStatusCommand.class));
        verify(idempotencyPort).completeWithOutcome("evt-6", ENDPOINT, "APPLIED");
    }

    @Test
    void partialFailureReturns503AndReleasesClaim() { // una cancelacion falla, las demas se conservan
        Reservation first = reservation(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 26));
        Reservation second = reservation(LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 27));
        when(repository.findReservedByVesselOverlapping(any(), any(), any()))
                .thenReturn(List.of(first.getId(), second.getId()));
        when(repository.findById(first.getId())).thenReturn(Optional.of(first));
        when(repository.findById(second.getId())).thenReturn(Optional.of(second));
        doThrow(new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "boom"))
                .when(changeReservationStatus).execute(org.mockito.ArgumentMatchers.argThat(
                        command -> command != null && first.getId().equals(command.reservationId())));

        assertThatThrownBy(() -> service.apply(command("evt-7", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00")))
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.MAINTENANCE_EVENT_PARTIAL_FAILURE.getCode()));

        verify(changeReservationStatus, org.mockito.Mockito.times(2)).execute(any(ChangeReservationStatusCommand.class));
        verify(idempotencyPort).releaseClaim("evt-7", ENDPOINT);
        verify(idempotencyPort, never()).completeWithOutcome(any(), any(), any());
    }

    @Test
    void maintenanceCommandCarriesSystemActorAndCause() { // comando acotado de C5
        Reservation reservation = reservation(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 26));
        when(repository.findReservedByVesselOverlapping(any(), any(), any())).thenReturn(List.of(reservation.getId()));
        when(repository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        service.apply(command("evt-8", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        org.mockito.ArgumentCaptor<ChangeReservationStatusCommand> captor =
                org.mockito.ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatus).execute(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(com.seashare.m2.reservas.domain.model.ActorType.SYSTEM);
        assertThat(captor.getValue().subtype()).isEqualTo(com.seashare.m2.reservas.domain.model.CancellationSubtype.BY_OWNER);
        assertThat(captor.getValue().cause()).isEqualTo(CancellationCause.MAINTENANCE);
    }

    @Test
    void missingUntilIsInvalidMaintenanceRange() { // D-05: until obligatorio
        assertThatThrownBy(() -> service.apply(new MaintenanceEventCommand(
                "evt-9", VESSEL_ID.toString(), null, null, "2026-11-19T15:15:00Z", "OWNER")))
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_MAINTENANCE_RANGE.getCode()));
    }

    @Test
    void untilNotAfterStartIsInvalidMaintenanceRange() { // until <= start
        assertThatThrownBy(() -> service.apply(command("evt-10", "2026-11-27T18:00:00-05:00", "2026-11-20T08:00:00-05:00")))
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_MAINTENANCE_RANGE.getCode()));
    }

    @Test
    void invalidVesselIdIsInvalidParameters() { // UUID malformado
        assertThatThrownBy(() -> service.apply(new MaintenanceEventCommand(
                "evt-11", "no-es-uuid", null, "2026-11-27T18:00:00-05:00", "2026-11-19T15:15:00Z", "OWNER")))
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
    }

    @Test
    void reservationOutsideTheWindowIsNotCancelled() { // el cruce se verifica con la zona del puerto
        Reservation outside = reservation(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        when(repository.findReservedByVesselOverlapping(any(), any(), any())).thenReturn(List.of(outside.getId()));
        when(repository.findById(outside.getId())).thenReturn(Optional.of(outside));

        MaintenanceEventResult result = service.apply(command("evt-12", "2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"));

        assertThat(result.outcome()).isEqualTo(MaintenanceEventOutcome.NO_AFFECTED_RESERVATIONS);
        verify(changeReservationStatus, never()).execute(any());
    }

    private MaintenanceEventCommand command(String eventId, String start, String until) {
        return new MaintenanceEventCommand(eventId, VESSEL_ID.toString(), start, until, "2026-11-19T15:15:00Z", "OWNER");
    }

    private Reservation reservation(LocalDate start, LocalDate end) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .vesselId(VESSEL_ID)
                .startDate(start)
                .endDate(end)
                .harborTimezone("America/Bogota")
                .build();
    }
}
