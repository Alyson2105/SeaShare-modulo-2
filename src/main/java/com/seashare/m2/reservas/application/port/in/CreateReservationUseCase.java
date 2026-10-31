package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.port.out.ReservationView;

public interface CreateReservationUseCase { // puerto de entrada para el caso de uso de creacion de reserva

    ReservationView execute(CreateReservationCommand command); // ejecuta la creacion formal de la reserva
}
