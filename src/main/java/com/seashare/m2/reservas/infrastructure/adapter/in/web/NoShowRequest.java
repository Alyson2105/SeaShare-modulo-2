package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NoShowRequest( // cuerpo opcional de CU-05

        @JsonProperty("observations")
        String observations // observaciones del propietario
) {
}