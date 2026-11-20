package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StatusAuditEntry( // registro inmutable para auditoria de estado
        UUID reservationId, // identificador de la reserva
        ReservationStatus fromStatus, // estado de origen; nulo en la creacion
        ReservationStatus toStatus, // estado de destino
        CancellationSubtype subtype, // subtipo de cancelacion efectivo
        ActorType actor, // actor que disparo la transicion
        String reason, // motivo del cambio
        Instant occurredAt, // marca temporal del cambio
        UUID actorId, // identificador del usuario; nulo si actor es SYSTEM
        List<UUID> eventIds // eventos encolados en la misma transaccion (FR-011); puede ser vacio
) {
    public StatusAuditEntry( // constructor compacto de compatibilidad sin actor_id ni event_ids
            UUID reservationId,
            ReservationStatus fromStatus,
            ReservationStatus toStatus,
            CancellationSubtype subtype,
            ActorType actor,
            String reason,
            Instant occurredAt
    ) {
        this(reservationId, fromStatus, toStatus, subtype, actor, reason, occurredAt, null, List.of()); // delega con valores por defecto
    }
}
