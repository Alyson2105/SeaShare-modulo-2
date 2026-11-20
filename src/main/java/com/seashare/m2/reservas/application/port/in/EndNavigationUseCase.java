package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.EndNavigationCommand;
import com.seashare.m2.reservas.application.dto.EndNavigationResult;

public interface EndNavigationUseCase { // CU-07: registrar el fin de navegacion

    EndNavigationResult execute(EndNavigationCommand command); // completa la reserva y registra el checkout
}