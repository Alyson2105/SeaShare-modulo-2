package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StartPaymentResult( // CU-03: resultado del inicio de pago
        UUID reservationId, // reserva transicionada
        ReservationStatus status, // PENDING_PAYMENT
        UUID vesselId, // embarcacion retenida
        CalculatedValue calculation, // desglose literal de M3
        long ttlRemainingSeconds, // segundos restantes del TTL original
        OffsetDateTime ttlExpiresAt, // vencimiento original en la zona del puerto
        int attempt // numero de intento de cobro
) {
}
