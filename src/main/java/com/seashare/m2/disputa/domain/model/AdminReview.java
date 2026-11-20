package com.seashare.m2.disputa.domain.model;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

/**
 * Revision administrativa de una disputa (CU-17 Key Entities).
 *
 * <p>Registro inmutable de la actuacion del Admin: decision, motivo opcional y
 * notas internas. No participa del evento hacia M3.</p>
 */
public record AdminReview(
        UUID id,
        UUID disputeId,
        UUID adminId,
        DisputeStatus decision,
        String reason,
        String internalNotes,
        Instant reviewedAt
) {

    public AdminReview { // invariantes de la revision
        if (disputeId == null || adminId == null || decision == null || reviewedAt == null) {
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS,
                    "disputeId, adminId, decision and reviewedAt are required for an admin review");
        }
    }

    public static AdminReview record(UUID disputeId,
                                     UUID adminId,
                                     DisputeStatus decision,
                                     String reason,
                                     String internalNotes,
                                     Instant reviewedAt) { // construye la fila de revision
        return new AdminReview(UUID.randomUUID(), disputeId, adminId, decision, reason, internalNotes, reviewedAt);
    }
}
