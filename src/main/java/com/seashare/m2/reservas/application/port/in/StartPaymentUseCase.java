package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;

public interface StartPaymentUseCase { // CU-03: iniciar el pago de una reserva INITIATED

    StartPaymentResult start(StartPaymentCommand command); // calcula, valida concurrencia y transiciona a PENDING_PAYMENT
}
