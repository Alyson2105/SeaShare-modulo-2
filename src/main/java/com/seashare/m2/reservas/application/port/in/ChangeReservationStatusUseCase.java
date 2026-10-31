package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;

public interface ChangeReservationStatusUseCase { // puerto de entrada para cambio de estado

    void execute(ChangeReservationStatusCommand command); // ejecuta la transicion de estado
}
