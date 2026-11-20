package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.CancellationResult;
import com.seashare.m2.reservas.application.dto.RequestCancellationCommand;

public interface RequestCancellationUseCase { // puerto de entrada para cancelar una reserva

    CancellationResult execute(RequestCancellationCommand command); // ejecuta la cancelacion y devuelve el contrato
}