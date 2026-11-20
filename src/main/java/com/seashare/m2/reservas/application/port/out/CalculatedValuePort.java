package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.CalculatedValue;

import java.util.UUID;

public interface CalculatedValuePort { // puerto de salida para el valor calculado de la reserva en modulo 3 (CU-12)

    CalculatedValue getCalculatedValue(UUID reservationId); // trae el desglose y total desde M3
}
