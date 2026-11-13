package com.seashare.m2.reservas.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptPort { // CU-13: historial de intentos de cobro con idempotencia

    boolean registerIfAbsent(UUID reservationId,
                             String externalReference,
                             String result,
                             String detail,
                             Instant consultedAt); // false si la clave (reserva, referencia, resultado) ya existia

    int countAttempts(UUID reservationId); // intentos registrados para numerar el siguiente

    Optional<String> lastResult(UUID reservationId); // ultimo resultado registrado
}
