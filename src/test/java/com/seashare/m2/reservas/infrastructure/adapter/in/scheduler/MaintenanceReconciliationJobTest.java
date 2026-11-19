package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.dto.FleetMaintenanceWindow;
import com.seashare.m2.reservas.application.dto.MaintenanceEventCommand;
import com.seashare.m2.reservas.application.port.in.ApplyMaintenanceEventUseCase;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaintenanceReconciliationJobTest { // job de respaldo de C5

    private static final Instant NOW = Instant.parse("2026-11-19T15:15:00Z");

    private static final UUID VESSEL_ID = UUID.fromString("37a31793-29e9-4765-9d3d-39b74a744476");

    private static final FleetMaintenanceWindow WINDOW = new FleetMaintenanceWindow(
            Instant.parse("2026-11-20T13:00:00Z"), Instant.parse("2026-11-27T23:00:00Z"));

    private ReservationRepositoryPort repository;

    private FleetOperationalStatusPort fleet;

    private ApplyMaintenanceEventUseCase applyMaintenanceEvent;

    private MaintenanceReconciliationJob job;

    @BeforeEach
    void setUp() {
        repository = mock(ReservationRepositoryPort.class);
        fleet = mock(FleetOperationalStatusPort.class);
        applyMaintenanceEvent = mock(ApplyMaintenanceEventUseCase.class);
        ClockPort clock = () -> NOW;
        job = new MaintenanceReconciliationJob(repository, fleet, applyMaintenanceEvent, clock, 60, 100);
        when(repository.findVesselsWithReservedBetween(any(), any())).thenReturn(List.of(VESSEL_ID));
    }

    @Test
    void synthesizesEventForUnavailableVesselWithWindow() { // C3 reporta mantenimiento con fechas
        when(fleet.isAvailable(any(), any(), any(), any())).thenReturn(false);
        when(fleet.maintenanceWindow(any(), any(), any(), any())).thenReturn(Optional.of(WINDOW));

        job.run();

        ArgumentCaptor<MaintenanceEventCommand> captor = ArgumentCaptor.forClass(MaintenanceEventCommand.class);
        verify(applyMaintenanceEvent).apply(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(MaintenanceReconciliationJob.eventId(VESSEL_ID, WINDOW));
        assertThat(captor.getValue().vesselId()).isEqualTo(VESSEL_ID.toString());
        assertThat(captor.getValue().reason()).isEqualTo("ADMIN");
        assertThat(captor.getValue().maintenanceUntil()).isEqualTo(WINDOW.until().toString());
    }

    @Test
    void skipsAvailableVessel() { // C3 dice disponible
        when(fleet.isAvailable(any(), any(), any(), any())).thenReturn(true);

        job.run();

        verify(fleet, never()).maintenanceWindow(any(), any(), any(), any());
        verify(applyMaintenanceEvent, never()).apply(any());
    }

    @Test
    void skipsMaintenanceWithoutWindow() { // sin fechas no se inventa una ventana
        when(fleet.isAvailable(any(), any(), any(), any())).thenReturn(false);
        when(fleet.maintenanceWindow(any(), any(), any(), any())).thenReturn(Optional.empty());

        job.run();

        verify(applyMaintenanceEvent, never()).apply(any());
    }

    @Test
    void skipsWindowAlreadyExpired() { // defensa: no se sintetiza un aviso vencido
        when(fleet.isAvailable(any(), any(), any(), any())).thenReturn(false);
        when(fleet.maintenanceWindow(any(), any(), any(), any())).thenReturn(Optional.of(
                new FleetMaintenanceWindow(NOW.minusSeconds(7200), NOW.minusSeconds(3600))));

        job.run();

        verify(applyMaintenanceEvent, never()).apply(any());
    }

    @Test
    void continuesAfterOneFailure() { // un fallo no detiene el ciclo
        when(fleet.isAvailable(any(), any(), any(), any())).thenReturn(false);
        when(fleet.maintenanceWindow(any(), any(), any(), any())).thenReturn(Optional.of(WINDOW));
        doThrow(new IllegalStateException("boom")).when(applyMaintenanceEvent).apply(any());

        job.run(); // no propaga la excepcion

        verify(applyMaintenanceEvent).apply(any());
    }

    @Test
    void ignoresVesselsOutsideBatchLimit() { // batch-size acota el ciclo
        MaintenanceReconciliationJob limited = new MaintenanceReconciliationJob(
                repository, fleet, applyMaintenanceEvent, () -> NOW, 60, 0);
        when(repository.findVesselsWithReservedBetween(any(), any())).thenReturn(List.of(VESSEL_ID));

        limited.run();

        verify(fleet, never()).isAvailable(any(), any(), any(), any());
    }
}
