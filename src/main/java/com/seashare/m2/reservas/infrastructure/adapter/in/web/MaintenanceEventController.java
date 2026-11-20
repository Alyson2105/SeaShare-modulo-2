package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.MaintenanceEventCommand;
import com.seashare.m2.reservas.application.dto.MaintenanceEventResult;
import com.seashare.m2.reservas.application.port.in.ApplyMaintenanceEventUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint interno C5: Modulo 1 avisa que una embarcacion entra en mantenimiento. */
@RestController
public class MaintenanceEventController {

    private final ApplyMaintenanceEventUseCase service; // caso de uso de aplicacion

    public MaintenanceEventController(ApplyMaintenanceEventUseCase service) { // constructor con dependencia
        this.service = service; // asigna el servicio
    }

    @PostMapping("/api/v1/internal/maintenance-events")
    public ResponseEntity<MaintenanceEventResponse> receive(@RequestBody MaintenanceEventRequest request) {
        MaintenanceEventResult result = service.apply(new MaintenanceEventCommand(
                request.eventId(),
                request.vesselId(),
                request.maintenanceStart(),
                request.maintenanceUntil(),
                request.occurredAt(),
                request.reason())); // delega la validacion y el procesamiento

        return ResponseEntity.ok(new MaintenanceEventResponse(result.eventId(), result.outcome().name())); // 200 {event_id,outcome}
    }
}
