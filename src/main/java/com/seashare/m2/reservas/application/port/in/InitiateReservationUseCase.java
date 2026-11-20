package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.InitiateReservationCommand;
import com.seashare.m2.reservas.domain.model.Reservation;

public interface InitiateReservationUseCase { // T-24: punto de entrada de creacion del motor CU-08

    Reservation initiate(InitiateReservationCommand command); // persiste, audita y encola en la misma transaccion
}
