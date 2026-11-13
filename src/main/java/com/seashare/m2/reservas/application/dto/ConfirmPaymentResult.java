package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ReservationStatus;

public record ConfirmPaymentResult( // resultado de procesar una confirmacion de cobro
        PaymentConfirmationOutcome outcome, // tratamiento aplicado
        ReservationStatus status // estado resultante de la reserva
) {
}
