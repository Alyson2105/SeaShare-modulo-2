package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;

import java.util.UUID;

public record ReportNoShowCommand( // datos de entrada de CU-05
        UUID reservationId, // reserva con posible inasistencia
        UUID callerId, // usuario autenticado que reporta
        ActorType actor, // rol traducido por el adaptador web
        String observations // observaciones opcionales del propietario
) {
}