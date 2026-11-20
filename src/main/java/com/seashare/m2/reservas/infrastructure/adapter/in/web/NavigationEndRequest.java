package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record NavigationEndRequest( // cuerpo opcional de CU-07

        @JsonProperty("actual_arrival_at")
        OffsetDateTime actualArrivalAt, // llegada real con offset

        @JsonProperty("remarks")
        String remarks // novedades del cierre
) {
}