package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record CancellationRequest( // cuerpo opcional de CU-04

        @JsonProperty("reason")
        @Size(max = 500, message = "reason accepts at most 500 characters")
        String reason, // motivo del propietario

        @JsonProperty("justification")
        @Size(max = 500)
        String justification // justificacion libre de hasta 500 caracteres
) {
}