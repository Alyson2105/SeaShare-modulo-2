package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.EndNavigationResult;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record NavigationEndResponse( // respuesta exitosa de CU-07

        @JsonProperty("reservation_id")
        UUID reservationId, // reserva completada

        @JsonProperty("status")
        String status, // estado terminal

        @JsonProperty("actual_arrival_at")
        String actualArrivalAt, // llegada real en ISO_OFFSET_DATE_TIME

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion asociada

        @JsonProperty("vessel_operational_status")
        String vesselOperationalStatus, // aviso a M1 o null

        @JsonProperty("dispute_id")
        UUID disputeId, // siempre null hasta CU-16

        @JsonProperty("dispute_window_expires_at")
        String disputeWindowExpiresAt, // siempre null hasta CU-16

        @JsonProperty("message")
        String message // resumen informativo
) {

    public static NavigationEndResponse from(EndNavigationResult result) { // construye el payload del contrato
        return new NavigationEndResponse(
                result.reservationId(), // reserva
                result.status().name(), // estado como texto
                format(result.actualArrivalAt()), // llegada formateada
                result.vesselId(), // embarcacion
                result.vesselOperationalStatus() == null ? null : result.vesselOperationalStatus().name(), // estado M1 o null
                result.disputeId(), // disputa null
                format(result.disputeWindowExpiresAt()), // ventana null
                result.message()); // mensaje
    }

    private static String format(OffsetDateTime value) { // formatea fechas con offset
        return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); // evita npe y conserva offset
    }
}