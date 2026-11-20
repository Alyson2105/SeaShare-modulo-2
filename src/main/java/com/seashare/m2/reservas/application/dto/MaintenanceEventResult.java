package com.seashare.m2.reservas.application.dto;

/** Aviso de mantenimiento ya validado y aplicado. */
public record MaintenanceEventResult(String eventId, MaintenanceEventOutcome outcome) {
}
