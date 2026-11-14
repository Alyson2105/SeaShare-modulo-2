package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.disputa.application.dto.DisputeListView;
import com.seashare.m2.disputa.domain.model.DisputeClaim;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeListResponse(
        @JsonProperty("disputes")
        List<DisputeResponse> disputes
) {

    public record DisputeResponse(
            @JsonProperty("dispute_id")
            UUID disputeId,
            @JsonProperty("reservation_id")
            UUID reservationId,
            @JsonProperty("status")
            String status,
            @JsonProperty("window_start")
            Instant windowStart,
            @JsonProperty("window_end")
            Instant windowEnd,
            @JsonProperty("claim")
            DisputeClaimResponse.ClaimBody claim
    ) {
    }

    public static DisputeListResponse from(List<DisputeListView> views) {
        return new DisputeListResponse(views.stream()
                .map(view -> new DisputeResponse(
                        view.disputeId(), view.reservationId(), view.status().name(),
                        view.windowStart(), view.windowEnd(), claimBody(view.claim())))
                .toList());
    }

    private static DisputeClaimResponse.ClaimBody claimBody(DisputeClaim claim) {
        if (claim == null) {
            return null;
        }
        return new DisputeClaimResponse.ClaimBody(
                claim.id(), claim.ownerId(), claim.description(),
                claim.category() == null ? null : claim.category().wireValue(),
                claim.evidenceUrls(), claim.registeredAt());
    }
}
