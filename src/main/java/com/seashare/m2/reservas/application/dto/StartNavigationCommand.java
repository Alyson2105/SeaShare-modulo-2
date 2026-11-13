package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StartNavigationCommand( // datos de entrada de CU-06
        UUID reservationId, // reserva que inicia la navegacion
        UUID callerId, // usuario autenticado que solicita el inicio
        ActorType actor, // rol traducido por el adaptador web
        OffsetDateTime actualDepartureAt, // salida real opcional
        String deliveryNotes // notas de entrega opcionales
) {
}