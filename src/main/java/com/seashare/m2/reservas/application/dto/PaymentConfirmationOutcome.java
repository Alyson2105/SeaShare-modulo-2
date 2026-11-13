package com.seashare.m2.reservas.application.dto;

public enum PaymentConfirmationOutcome { // tratamiento aplicado a una confirmacion de cobro

    CONFIRMED, // aprobado dentro del TTL: la reserva pasa a RESERVED

    PENDING_RETRY, // rechazado con TTL vigente: sigue PENDING_PAYMENT

    PAYMENT_FAILED, // resultado definitivo negativo: la reserva pasa a PAYMENT_FAILED

    LATE_REVERSAL, // aprobado fuera del TTL: se instruye la reversion y el estado no cambia

    DUPLICATE, // la clave (reserva, referencia, resultado) ya se habia procesado

    IGNORED // sin resultado definitivo o estado no aplicable
}
