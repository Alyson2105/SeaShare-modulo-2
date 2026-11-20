package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.StartNavigationCommand;
import com.seashare.m2.reservas.application.dto.StartNavigationResult;

public interface StartNavigationUseCase { // CU-06: registrar el inicio de navegacion

    StartNavigationResult execute(StartNavigationCommand command); // inicia la reserva y registra el check-in
}