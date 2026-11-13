package com.seashare.m2.reservas.application.dto;

import java.util.UUID;

public record StartPaymentCommand( // CU-03: datos validados para iniciar el pago
        UUID reservationId, // reserva en INITIATED
        UUID requesterId, // arrendatario titular
        String role, // perfil del llamador
        String paymentTokenRef // token de un solo uso generado en el frontend
) {
}
