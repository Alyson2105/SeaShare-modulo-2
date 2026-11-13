package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.ReportNoShowResult;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record NoShowResponse( // respuesta exitosa de CU-05

        @JsonProperty("reservation_id")
        UUID reservationId, // reserva cancelada

        @JsonProperty("status")
        String status, // estado resultante

        @JsonProperty("cancellation_subtype")
        String cancellationSubtype, // subtipo NO_SHOW

        @JsonProperty("cancelled_at")
        String cancelledAt, // cancelacion en ISO_OFFSET_DATE_TIME

        @JsonProperty("wait_minutes_recorded")
        long waitMinutesRecorded, // minutos completos esperados

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion asociada

        @JsonProperty("vessel_operational_status")
        String vesselOperationalStatus, // aviso a M1 o null

        @JsonProperty("message")
        String message // resumen informativo
) {

    public static NoShowResponse from(ReportNoShowResult result) { // construye el payload del contrato
        return new NoShowResponse(
                result.reservationId(), // reserva
                result.status().name(), // estado como texto
                result.cancellationSubtype().name(), // subtipo como texto
                result.cancelledAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), // cancelacion formateada
                result.waitMinutesRecorded(), // minutos esperados
                result.vesselId(), // embarcacion
                result.vesselOperationalStatus() == null ? null : result.vesselOperationalStatus().name(), // estado M1 o null
                result.message()); // mensaje
    }
}