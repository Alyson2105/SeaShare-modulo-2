package com.seashare.m2.disputa.domain.model;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reclamo del propietario sobre una disputa (CU-16).
 *
 * <p>Solo datos facticos: descripcion, categoria y evidencias. No hay montos ni
 * valoraciones economicas (CU-16 FR-010 y SC-004). El reclamo es inmutable y se
 * admite uno solo por disputa (D-08).</p>
 */
public record DisputeClaim(
        UUID id,
        UUID disputeId,
        UUID ownerId,
        String description,
        DisputeCategory category,
        List<String> evidenceUrls,
        Instant registeredAt
) {

    public static final int MIN_DESCRIPTION_LENGTH = 10; // contrato CU-16

    public static final int MAX_DESCRIPTION_LENGTH = 2000; // contrato CU-16

    public static final int MAX_EVIDENCE_URLS = 5; // contrato CU-16

    public DisputeClaim { // valida las invariantes del reclamo
        if (description == null || description.isBlank()
                || description.length() < MIN_DESCRIPTION_LENGTH
                || description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS,
                    "description is required and must contain between 10 and 2000 characters");
        }
        if (evidenceUrls != null && evidenceUrls.size() > MAX_EVIDENCE_URLS) {
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS,
                    "evidence_urls accepts at most 5 entries");
        }
        evidenceUrls = evidenceUrls == null ? List.of() : List.copyOf(evidenceUrls);
    }

    public static DisputeClaim register(UUID disputeId,
                                        UUID ownerId,
                                        String description,
                                        DisputeCategory category,
                                        List<String> evidenceUrls,
                                        Instant registeredAt) { // crea el reclamo con identificador propio
        return new DisputeClaim(UUID.randomUUID(), disputeId, ownerId, description, category, evidenceUrls, registeredAt);
    }
}
