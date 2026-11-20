package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterDisputeClaimRequest( // cuerpo de POST /api/v1/disputes/{id}/claims (CU-16)

        @NotBlank(message = "description is required")
        @Size(min = 10, max = 2000, message = "description must contain between 10 and 2000 characters")
        @JsonProperty("description")
        String description, // relato factico del dano o problema

        @JsonProperty("damage_category")
        String damageCategory, // opcional: structure, engine, equipment, cleaning, delay, other

        @Size(max = 5, message = "evidence_urls accepts at most 5 entries")
        @JsonProperty("evidence_urls")
        List<String> evidenceUrls // opcional: enlaces de evidencia
) {
}
