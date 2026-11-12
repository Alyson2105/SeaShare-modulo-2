package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.NewReservation;

public record InitiateReservationCommand( // T-24: entrada de creacion al motor CU-08
        NewReservation newReservation, // datos inmutables de la reserva a persistir
        int maxCapacity // capacidad maxima de M1, necesaria en reservation.info.provided
) { // la creacion no reutiliza el execute generico de cambio de estado
}
