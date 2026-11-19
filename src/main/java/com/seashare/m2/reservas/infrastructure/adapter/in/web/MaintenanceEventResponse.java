package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de C5: identificador del aviso y resultado aplicado (contrato M1-M2 §6). */
public record MaintenanceEventResponse(

        @JsonProperty("event_id")
        String eventId,

        @JsonProperty("outcome")
        String outcome
) {
}
