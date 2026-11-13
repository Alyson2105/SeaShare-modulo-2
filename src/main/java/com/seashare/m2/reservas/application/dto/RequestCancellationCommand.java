package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;

import java.util.UUID;

public record RequestCancellationCommand( // datos de entrada para solicitar una cancelacion
        UUID reservationId, // reserva que se desea cancelar
        UUID callerId, // usuario autenticado que realiza la solicitud
        ActorType actor, // rol del solicitante: RENTER u OWNER
        String reason, // motivo opcional del propietario
        String justification // justificacion opcional
) {
}