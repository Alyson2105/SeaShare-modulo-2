package com.seashare.m2.reservas.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyPort { // puerto de salida para gestion atomica de idempotencia

    boolean tryClaim(String key, String endpoint, Instant now, Duration window); // intenta reclamar atómicamente la clave para el endpoint

    void complete(String key, String endpoint, UUID reservationId); // asocia la reserva creada con la clave de idempotencia

    Optional<UUID> findReservationId(String key, String endpoint); // obtiene la reserva asociada a una clave existente
}
