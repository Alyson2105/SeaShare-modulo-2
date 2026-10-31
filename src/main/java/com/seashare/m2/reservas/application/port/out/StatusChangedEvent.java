package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusChangedEvent( // evento de dominio para encolado outbox
        UUID eventId, // identificador unico del evento
        UUID reservationId, // identificador de la reserva
        ReservationStatus status, // nuevo estado de la reserva
        CancellationSubtype subtype, // subtipo de cancelacion efectivo
        Instant changedAt // marca temporal del cambio de estado
) {
}
