package com.seashare.m2.reservas.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ConfirmPaymentCommand( // CU-13: resultado del cobro ya normalizado (ambos canales)
        UUID reservationId, // reserva notificada
        String externalTransactionId, // referencia externa de la transaccion
        PaymentStatus result, // estado del cobro
        Instant processedAt, // instante en que M2 obtuvo el resultado
        String detail // motivo o codigo de la pasarela
) {
}
