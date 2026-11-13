package com.seashare.m2.disputa.application.dto;

import com.seashare.m2.disputa.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

public record UpdateDisputeStatusResult( // resultado de la transicion (CU-17)
        UUID disputeId,
        UUID reservationId,
        DisputeStatus previousStatus,
        DisputeStatus newStatus,
        UUID resolvedBy,
        Instant resolvedAt,
        String reason,
        boolean m3EventPublished // encolado en outbox dentro de la misma transaccion
) {
}
