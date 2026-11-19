package com.seashare.m2.reservas.application.dto;

/** Comando de aplicacion con los campos crudos del aviso C5; la validacion vive en el servicio. */
public record MaintenanceEventCommand(
        String eventId,
        String vesselId,
        String maintenanceStart,
        String maintenanceUntil,
        String occurredAt,
        String reason
) {
}
