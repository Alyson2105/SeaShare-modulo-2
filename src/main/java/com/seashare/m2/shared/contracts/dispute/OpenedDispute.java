package com.seashare.m2.shared.contracts.dispute;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de abrir (o recuperar) la disputa de una reserva (H9, T-18a).
 *
 * @param disputeId     identificador de la disputa
 * @param reservationId reserva asociada
 * @param status        estado de la disputa; siempre PENDING al abrirse
 * @param windowStart   inicio de la ventana de reclamo
 * @param windowEnd     fin de la ventana de reclamo (windowStart + 24 h)
 */
public record OpenedDispute(
        UUID disputeId,
        UUID reservationId,
        String status,
        Instant windowStart,
        Instant windowEnd
) {
}
