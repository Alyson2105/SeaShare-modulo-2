package com.seashare.m2.disputa.application.dto;

import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

public record DisputeListView(
        UUID disputeId,
        UUID reservationId,
        DisputeStatus status,
        Instant windowStart,
        Instant windowEnd,
        DisputeClaim claim
) {
}
