package com.seashare.m2.shared.contracts.dispute;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos minimos para abrir la disputa de una reserva completada (H9, T-18a).
 *
 * <p>Los tres valores los tiene CU-07 en su propio agregado, de modo que
 * {@code disputa} no necesita leer el contexto {@code reservas}:</p>
 *
 * @param reservationId reserva que paso a COMPLETED (obligatorio; clave de idempotencia)
 * @param ownerId       propietario registrado de la embarcacion (obligatorio; autoriza el reclamo)
 * @param completedAt   instante del paso a COMPLETED; inicio de la ventana de 24 h (obligatorio)
 */
public record OpenDisputeCommand(
        UUID reservationId,
        UUID ownerId,
        Instant completedAt
) {
}
