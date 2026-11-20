package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.disputa.application.dto.DisputeClaimView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeClaimResponse( // respuesta 201 del reclamo (CU-16 plan seccion 4.2)

        @JsonProperty("dispute_id")
        UUID disputeId,

        @JsonProperty("reservation_id")
        UUID reservationId,

        @JsonProperty("status")
        String status,

        @JsonProperty("claim")
        ClaimBody claim,

        @JsonProperty("auto_close_disabled")
        boolean autoCloseDisabled,

        @JsonProperty("window_24h")
        WindowBody window24h,

        @JsonProperty("message")
        String message
) {

    private static final String REGISTERED_MESSAGE =
            "Tu reclamo ha quedado formalmente registrado y derivado a la revision administrativa. "
                    + "La disputa continua en PENDING hasta su resolucion. "
                    + "No se aplico ningun debito ni calculo monetario.";

    public record ClaimBody( // objeto claim del contrato

            @JsonProperty("claim_id")
            UUID claimId,

            @JsonProperty("owner_id")
            UUID ownerId,

            @JsonProperty("description")
            String description,

            @JsonProperty("damage_category")
            String damageCategory,

            @JsonProperty("evidence_urls")
            List<String> evidenceUrls,

            @JsonProperty("registered_at")
            Instant registeredAt
    ) {
    }

    public record WindowBody( // metadatos de la ventana de 24 h

            @JsonProperty("start_at")
            Instant startAt,

            @JsonProperty("end_at")
            Instant endAt,

            @JsonProperty("window_status")
            String windowStatus
    ) {
    }

    public static DisputeClaimResponse from(DisputeClaimView view) { // proyecta la vista al contrato snake_case
        return new DisputeClaimResponse(
                view.disputeId(),
                view.reservationId(),
                view.status().name(),
                new ClaimBody(view.claimId(), view.ownerId(), view.description(),
                        view.damageCategory(), view.evidenceUrls(), view.registeredAt()),
                view.autoCloseDisabled(),
                new WindowBody(view.windowStart(), view.windowEnd(), "OPEN_FOR_EDIT"),
                REGISTERED_MESSAGE);
    }

    public static DisputeClaimResponse edited(DisputeClaimView view) {
        return new DisputeClaimResponse(view.disputeId(), view.reservationId(), view.status().name(),
                new ClaimBody(view.claimId(), view.ownerId(), view.description(), view.damageCategory(),
                        view.evidenceUrls(), view.registeredAt()), view.autoCloseDisabled(),
                new WindowBody(view.windowStart(), view.windowEnd(), "OPEN_FOR_EDIT"),
                "Reclamo actualizado; se conserva el historial de versiones.");
    }
}
