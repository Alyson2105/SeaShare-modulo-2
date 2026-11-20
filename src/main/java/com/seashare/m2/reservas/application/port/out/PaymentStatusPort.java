package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.PaymentStatusResult;

import java.util.UUID;

public interface PaymentStatusPort { // CU-13 canal B: consulta el estado del cobro a modulo 3

    PaymentStatusResult getStatus(UUID reservationId); // GET UC06; nunca asume un resultado
}
