package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.InternalReservationView;

import java.util.UUID;

public interface GetInternalReservationUseCase { // puerto de entrada para consultar una reserva desde Modulo 3

    InternalReservationView execute(UUID reservationId); // devuelve los datos operativos de la reserva
}
