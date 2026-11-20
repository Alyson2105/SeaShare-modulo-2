package com.seashare.m2.disputa.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Comando para registrar el reclamo del propietario (CU-16 FR-006).
 *
 * <p>La categoria llega como texto del contrato y se valida en el caso de uso
 * con {@link com.seashare.m2.disputa.domain.model.DisputeCategory}.</p>
 */
public record RegisterDisputeClaimCommand(
        UUID disputeId, // disputa sobre la que se reclama
        UUID claimantId, // propietario autenticado que radica el reclamo
        String description, // relato factico del dano o problema
        String damageCategory, // categoria opcional (structure, engine, ...)
        List<String> evidenceUrls // enlaces opcionales de evidencia (maximo 5)
) {
}
