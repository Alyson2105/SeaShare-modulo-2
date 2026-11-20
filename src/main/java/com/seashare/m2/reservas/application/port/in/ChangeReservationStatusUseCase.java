package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;

public interface ChangeReservationStatusUseCase { // puerto de entrada para cambio de estado

    ChangeReservationStatusResult execute(ChangeReservationStatusCommand command); // ejecuta la transicion y devuelve el resultado
}
