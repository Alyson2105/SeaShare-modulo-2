package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo de C5: aviso de mantenimiento enviado por Modulo 1 (contrato M1-M2 §6). */
public record MaintenanceEventRequest(

        @JsonProperty("event_id")
        String eventId, // clave de idempotencia, texto <= 100

        @JsonProperty("vessel_id")
        String vesselId, // UUID de la embarcacion

        @JsonProperty("maintenance_start")
        String maintenanceStart, // ISO-8601 con offset; nulo = ahora

        @JsonProperty("maintenance_until")
        String maintenanceUntil, // ISO-8601 con offset; obligatorio

        @JsonProperty("occurred_at")
        String occurredAt, // ISO-8601 con offset; obligatorio

        @JsonProperty("reason")
        String reason // OWNER o ADMIN; informativo
) {
}
