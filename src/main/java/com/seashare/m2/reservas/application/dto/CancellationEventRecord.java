package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;

import java.time.Instant;
import java.util.UUID;

public record CancellationEventRecord( // fila a persistir en cancellation_event
        UUID reservationId, // reserva cancelada
        ActorType actor, // actor que cancela
        Instant requestedAt, // instante de la solicitud
        long anticipationMinutes, // minutos de anticipacion; 0 para OWNER
        CancellationSubtype cancellationSubtype, // clasificacion aplicada
        String reason, // motivo del propietario; null para RENTER
        String justification // justificacion normalizada
) {
}