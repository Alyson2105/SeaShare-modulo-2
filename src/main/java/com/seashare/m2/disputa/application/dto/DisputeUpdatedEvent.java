package com.seashare.m2.disputa.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de estado final de la disputa hacia Modulo 3 (CU-18).
 *
 * <p>Payload minimalista: identificadores, estado financiero, marca temporal y
 * clave de evento. Sin montos, sin motivo y sin identificadores de usuarios
 * (CU-18 FR-003 y FR-006).</p>
 */
public record DisputeUpdatedEvent(
        UUID eventId, // identificador unico determinista (Message-Id y UNIQUE del outbox)
        UUID reservationId, // reserva asociada
        UUID disputeId, // disputa resuelta
        String financeStatus, // COMPLETED o REJECTED, segun el contrato de M3
        Instant statusChangedAt, // instante de la resolucion en UTC
        String eventKey // disputa-<id>-<estado>, mismo valor en cualquier reenvio
) {
}
