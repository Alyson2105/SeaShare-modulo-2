package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.StartNavigationResult;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record NavigationStartResponse( // respuesta exitosa de CU-06

        @JsonProperty("reservation_id")
        UUID reservationId, // reserva en navegacion

        @JsonProperty("status")
        String status, // estado resultante

        @JsonProperty("actual_departure_at")
        String actualDepartureAt, // salida real en ISO_OFFSET_DATE_TIME

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion asociada

        @JsonProperty("vessel_operational_status")
        String vesselOperationalStatus, // aviso a M1 o null

        @JsonProperty("insurance_activated")
        boolean insuranceActivated, // true si se encolo el evento a M3

        @JsonProperty("message")
        String message // resumen informativo
) {

    public static NavigationStartResponse from(StartNavigationResult result) { // construye el payload del contrato
        return new NavigationStartResponse(
                result.reservationId(), // reserva
                result.status().name(), // estado como texto
                format(result.actualDepartureAt()), // salida formateada
                result.vesselId(), // embarcacion
                result.vesselOperationalStatus() == null ? null : result.vesselOperationalStatus().name(), // estado M1 o null
                result.insuranceActivated(), // seguro activado
                result.message()); // mensaje
    }

    private static String format(OffsetDateTime value) { // formatea fechas con offset
        return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); // evita npe y conserva offset
    }
}