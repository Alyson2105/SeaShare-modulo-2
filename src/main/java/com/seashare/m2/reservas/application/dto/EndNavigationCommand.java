package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EndNavigationCommand( // datos de entrada de CU-07
        UUID reservationId, // reserva que termina la navegacion
        UUID callerId, // usuario autenticado que solicita el cierre
        ActorType actor, // rol traducido por el adaptador web
        OffsetDateTime actualArrivalAt, // llegada real opcional
        String remarks // novedades opcionales
) {
}