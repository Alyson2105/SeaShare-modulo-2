package com.seashare.m2.shared.contracts.dispute;

/**
 * Contrato interno de apertura de disputa de garantia (H9, T-18a).
 *
 * <p>Frontera entre los contextos {@code reservas} y {@code disputa}: lo
 * implementa {@code disputa} y lo invoca {@code reservas} (CU-07) sin que
 * ninguno de los dos conozca la estructura interna del otro (D-03).</p>
 *
 * <p>Reglas del contrato:</p>
 * <ul>
 *   <li>La implementacion es transaccional (REQUIRED): si CU-07 ya esta en una
 *       transaccion, la disputa se crea atomicamente con el paso a COMPLETED.</li>
 *   <li>Es idempotente: si la reserva ya tiene disputa, devuelve la existente
 *       aunque el comando traiga datos distintos (protege el reintento de CU-07).</li>
 *   <li>Crea la disputa en estado PENDING con una ventana fija de 24 horas
 *       contada desde {@code completedAt}.</li>
 * </ul>
 */
public interface OpenDisputeUseCase { // apertura automatica de la disputa de garantia

    OpenedDispute open(OpenDisputeCommand command); // devuelve la disputa creada o la ya existente
}
