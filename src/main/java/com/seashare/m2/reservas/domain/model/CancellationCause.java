package com.seashare.m2.reservas.domain.model;

/**
 * Causa funcional de una cancelacion.
 *
 * <p>Las causas humanas ya se deducen del actor y del subtipo; {@code MAINTENANCE} identifica
 * la cancelacion automatica disparada por el aviso C5 de Modulo 1 (contrato M1-M2 §6).</p>
 */
public enum CancellationCause {
    RENTER, // cancelacion solicitada por el arrendatario
    OWNER, // cancelacion solicitada por el propietario
    NO_SHOW, // inasistencia reportada por el propietario
    LATE_PAYMENT, // reversion por pago tardio
    MAINTENANCE // cancelacion automatica por mantenimiento de la embarcacion
}
