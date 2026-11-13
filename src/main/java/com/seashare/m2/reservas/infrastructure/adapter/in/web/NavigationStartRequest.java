package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record NavigationStartRequest( // cuerpo opcional de CU-06

        @JsonProperty("actual_departure_at")
        OffsetDateTime actualDepartureAt, // salida real con offset

        @JsonProperty("delivery_notes")
        String deliveryNotes // notas de entrega en muelle
) {
}