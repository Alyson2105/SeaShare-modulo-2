package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusChangedEvent( // evento de dominio para encolado outbox
        UUID eventId, // identificador unico del evento
        UUID reservationId, // identificador de la reserva
        ReservationStatus status, // nuevo estado de la reserva
        CancellationSubtype subtype, // subtipo de cancelacion efectivo
        Instant changedAt, // marca temporal del cambio de estado
        ActorType actor, // actor que disparo la transicion (CU-14 FR-002)
        Integer anticipationMinutes, // anticipacion cronologica al cancelar (CU-14 FR-003)
        String completionNotes, // notas de cierre en COMPLETED (CU-14 FR-004)
        String paymentTokenRef // token de pago opaco hacia M3; solo se publica hacia PENDING_PAYMENT
) {
    public StatusChangedEvent( // constructor compacto de compatibilidad (sin datos de CU-14)
            UUID eventId,
            UUID reservationId,
            ReservationStatus status,
            CancellationSubtype subtype,
            Instant changedAt
    ) {
        this(eventId, reservationId, status, subtype, changedAt, null, null, null, null); // delega con opcionales nulos
    }

    public StatusChangedEvent( // constructor de compatibilidad sin token de pago
            UUID eventId,
            UUID reservationId,
            ReservationStatus status,
            CancellationSubtype subtype,
            Instant changedAt,
            ActorType actor,
            Integer anticipationMinutes,
            String completionNotes
    ) {
        this(eventId, reservationId, status, subtype, changedAt, actor, anticipationMinutes, completionNotes, null); // delega con token nulo
    }
}
