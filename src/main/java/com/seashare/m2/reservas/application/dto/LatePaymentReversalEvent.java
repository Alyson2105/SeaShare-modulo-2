package com.seashare.m2.reservas.application.dto;

import java.time.Instant;
import java.util.UUID;

public record LatePaymentReversalEvent( // CU-13 FR-006: instruccion de reversion al cobro tardio
        UUID eventId, // identificador para deduplicacion en M3
        UUID reservationId, // reserva que permanece EXPIRED en M2
        Instant changedAt // instante en que se detecto el cobro tardio
) {
}
