package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.ConfirmPaymentResult;

public interface ConfirmPaymentUseCase { // CU-13: procesa el resultado del cobro

    ConfirmPaymentResult confirm(ConfirmPaymentCommand command); // idempotente por (referencia, resultado)
}
