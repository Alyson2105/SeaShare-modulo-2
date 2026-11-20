package com.seashare.m2.reservas.application.dto;

import java.util.UUID;

public record StartPaymentCommand( // CU-03: datos validados para iniciar el pago
        UUID reservationId, // reserva en INITIATED
        UUID requesterId, // arrendatario titular
        String role, // perfil del llamador
        String paymentTokenRef, // token de un solo uso generado en el frontend
        String paymentMethodType, // tipo del medio de pago hacia M3
        String payerEmail, // correo del pagador hacia M3
        String lastFour // ultimos cuatro digitos hacia M3
) {
    public StartPaymentCommand( // constructor compacto de compatibilidad (tests existentes)
            UUID reservationId,
            UUID requesterId,
            String role,
            String paymentTokenRef
    ) {
        this(reservationId, requesterId, role, paymentTokenRef, null, null, null); // delega con opcionales nulos
    }
}
