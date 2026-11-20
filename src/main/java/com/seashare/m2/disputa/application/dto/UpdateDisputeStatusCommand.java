package com.seashare.m2.disputa.application.dto;

import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;

import java.util.UUID;

/**
 * Comando de cambio de estado de la disputa (CU-17).
 *
 * <p>Lo usan tanto el Admin ({@code actorType = ADMIN}) como el cierre automatico
 * por vencimiento ({@code actorType = SYSTEM}, sin {@code adminId}), de modo que
 * existe una sola fuente de verdad para las transiciones (CU-16 FR-004/FR-008).</p>
 */
public record UpdateDisputeStatusCommand(
        UUID disputeId, // disputa a resolver
        DisputeStatus targetStatus, // PENDING, REJECTED o ACCEPTED
        UUID adminId, // revisor; nulo en el cierre de sistema
        String reason, // motivo informativo local; nunca viaja a M3
        String internalNotes, // notas internas de auditoria
        DisputeActorType actorType // ADMIN o SYSTEM
) {
}
