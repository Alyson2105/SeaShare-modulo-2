package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusAuditEntry( // registro inmutable para auditoria de estado
        UUID reservationId, // identificador de la reserva
        ReservationStatus fromStatus, // estado de origen
        ReservationStatus toStatus, // estado de destino
        CancellationSubtype subtype, // subtipo de cancelacion efectivo
        ActorType actor, // actor que disparo la transicion
        String reason, // motivo del cambio
        Instant occurredAt // marca temporal del cambio
) {
}
