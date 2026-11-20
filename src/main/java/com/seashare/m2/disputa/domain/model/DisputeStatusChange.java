package com.seashare.m2.disputa.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila del historial de estados de una disputa (tabla dispute_status_audit).
 *
 * <p>Es append-only: la tabla tiene un trigger que impide UPDATE y DELETE.</p>
 */
public record DisputeStatusChange(
        UUID id,
        UUID disputeId,
        DisputeStatus fromStatus,
        DisputeStatus toStatus,
        DisputeActorType actorType,
        UUID actorId,
        String reason,
        Instant occurredAt
) {

    public static DisputeStatusChange record(UUID disputeId,
                                             DisputeStatus fromStatus,
                                             DisputeStatus toStatus,
                                             DisputeActorType actorType,
                                             UUID actorId,
                                             String reason,
                                             Instant occurredAt) { // construye una fila nueva del historial
        return new DisputeStatusChange(UUID.randomUUID(), disputeId, fromStatus, toStatus, actorType, actorId, reason, occurredAt);
    }
}
