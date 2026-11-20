package com.seashare.m2.disputa.application.dto;

import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeClaimView( // vista de aplicacion para armar la respuesta del reclamo
        UUID disputeId,
        UUID reservationId,
        DisputeStatus status,
        UUID claimId,
        UUID ownerId,
        String description,
        String damageCategory,
        List<String> evidenceUrls,
        Instant registeredAt,
        Instant windowStart,
        Instant windowEnd,
        boolean autoCloseDisabled
) {

    public static DisputeClaimView of(GuaranteeDispute dispute, DisputeClaim claim) { // combina disputa y reclamo
        return new DisputeClaimView(
                dispute.id(),
                dispute.reservationId(),
                dispute.status(),
                claim.id(),
                claim.ownerId(),
                claim.description(),
                claim.category() == null ? null : claim.category().wireValue(),
                claim.evidenceUrls(),
                claim.registeredAt(),
                dispute.window().start(),
                dispute.window().end(),
                dispute.hasClaim());
    }
}
