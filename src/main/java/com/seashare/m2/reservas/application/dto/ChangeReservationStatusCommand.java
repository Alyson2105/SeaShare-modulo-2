package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.util.UUID;

public record ChangeReservationStatusCommand( // comando de aplicacion para cambiar estado
        UUID reservationId, // identificador de la reserva
        ReservationStatus targetStatus, // estado de destino requerido
        CancellationSubtype subtype, // subtipo de cancelacion opcional
        ActorType actor, // actor que solicita la operacion
        String reason // motivo o justificacion del cambio
) {
}
