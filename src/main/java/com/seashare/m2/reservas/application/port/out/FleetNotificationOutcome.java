package com.seashare.m2.reservas.application.port.out;

/**
 * Resultado de la notificacion de estado operativo hacia M1 (T-03, D-01/D-05).
 *
 * <ul>
 *   <li>{@link #DELIVERED}: M1 acepto la notificacion (2xx) -> se marca publicado.</li>
 *   <li>{@link #RETRYABLE_FAILURE}: fallo transitorio (5xx, timeout) -> reintento con backoff.</li>
 *   <li>{@link #PERMANENT_FAILURE}: 4xx (D-05: todo 4xx es permanente) -> dead directo, sin reintento.</li>
 * </ul>
 */
public enum FleetNotificationOutcome { // resultado de la llamada al puerto de notificacion a M1

    DELIVERED, // M1 confirmo la recepcion

    RETRYABLE_FAILURE, // fallo transitorio: reintentar con backoff

    PERMANENT_FAILURE // 4xx permanente: dead directo sin reintento (D-05)
}
