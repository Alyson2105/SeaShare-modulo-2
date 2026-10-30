package com.seashare.m2.reservas.domain.model;

public enum ReservationStatus { // estados de una reserva
    INITIATED, // creada, aun sin pago iniciado
    PENDING_PAYMENT, // pago en curso, TTL de 15 min vigente
    RESERVED, // pago confirmado
    IN_NAVIGATION, // navegacion en curso
    COMPLETED, // final: navegacion terminada
    CANCELLED, // final: requiere CancellationSubtype
    EXPIRED, // final: vencio el TTL
    PAYMENT_FAILED; // final: M3 reporto cobro cancelado o expirado

    public boolean isFinal() { // true si no tiene salida
        return this == COMPLETED || this == CANCELLED || this == EXPIRED || this == PAYMENT_FAILED; // los 4 finales
    }
}