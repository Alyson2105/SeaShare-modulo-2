package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeUpdatedEvent;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Traduccion del estado interno de la disputa al contrato de M3 (CU-18).
 *
 * <p>ACCEPTED se publica como COMPLETED y REJECTED como REJECTED
 * (CU-18 plan seccion 4). El eventId es determinista a partir del eventKey para
 * que un reenvio no duplique el evento.</p>
 */
public final class DisputeEventMapper {

    static final String ACCEPTED_FINANCE_STATUS = "COMPLETED"; // M2 ACCEPTED -> M3 COMPLETED

    static final String REJECTED_FINANCE_STATUS = "REJECTED"; // M2 REJECTED -> M3 REJECTED

    private DisputeEventMapper() { // clase de utilidades
    }

    public static DisputeUpdatedEvent finalEvent(GuaranteeDispute dispute, Instant changedAt) { // solo estados finales
        if (!dispute.status().isFinal()) { // PENDING nunca genera mensaje (CU-18 FR-001)
            throw new IllegalArgumentException("Only final disputes produce a finance event");
        }
        String financeStatus = dispute.status() == DisputeStatus.ACCEPTED
                ? ACCEPTED_FINANCE_STATUS
                : REJECTED_FINANCE_STATUS;
        String eventKey = "disputa-" + dispute.id() + "-" + financeStatus; // formato del contrato CU-18
        UUID eventId = UUID.nameUUIDFromBytes(eventKey.getBytes(StandardCharsets.UTF_8)); // determinista
        return new DisputeUpdatedEvent(eventId, dispute.reservationId(), dispute.id(),
                financeStatus, changedAt, eventKey);
    }
}
