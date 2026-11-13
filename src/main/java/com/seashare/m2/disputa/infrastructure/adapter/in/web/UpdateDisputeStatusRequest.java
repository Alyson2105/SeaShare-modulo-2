package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisputeStatusRequest( // cuerpo de PUT /api/v1/disputes/{id}/status (CU-17)

        @NotBlank(message = "new_status is required")
        @JsonProperty("new_status")
        String newStatus, // ACCEPTED, REJECTED o PENDING

        @Size(max = 1000, message = "reason accepts at most 1000 characters")
        @JsonProperty("reason")
        String reason, // motivo opcional, solo informativo local

        @Size(max = 1000, message = "internal_notes accepts at most 1000 characters")
        @JsonProperty("internal_notes")
        String internalNotes // notas internas del Admin
) {
}
