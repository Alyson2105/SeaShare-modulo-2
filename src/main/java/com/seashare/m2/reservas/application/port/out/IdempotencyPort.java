package com.seashare.m2.reservas.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyPort { // puerto de salida para gestion atomica de idempotencia

    boolean tryClaim(String key, String endpoint, Instant now, Duration window); // intenta reclamar atómicamente la clave para el endpoint

    void complete(String key, String endpoint, UUID reservationId); // asocia la reserva creada con la clave de idempotencia

    Optional<UUID> findReservationId(String key, String endpoint); // obtiene la reserva asociada a una clave existente

    /**
     * Reclama una clave sin ventana de expiracion. Se usa en C5: un {@code event_id} ya procesado
     * debe seguir siendo idempotente indefinidamente (contrato M1-M2 §6).
     */
    boolean tryClaimPermanent(String key, String endpoint, Instant now);

    /**
     * Guarda el resultado de un evento ya procesado ({@code APPLIED}, {@code DUPLICATE}, ...).
     * La columna reutilizada es {@code idempotency_key.response_ref}.
     */
    void completeWithOutcome(String key, String endpoint, String outcome);

    /** Recupera el resultado guardado de un evento, si ya se proceso por completo. */
    Optional<String> findOutcome(String key, String endpoint);

    /** Libera un reclamo sin resultado: se usa cuando C5 falla parcialmente y M1 debe reintentar. */
    void releaseClaim(String key, String endpoint);
}
