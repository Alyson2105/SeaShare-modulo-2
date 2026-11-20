package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;

public record CalculatedValue( // desglose del valor calculado devuelto por modulo 3, montos opacos sin aritmetica

        BigDecimal rentalAmount, // alquiler tal cual de M3; M2 nunca lo calcula

        BigDecimal insuranceAmount, // seguro tal cual de M3; M2 nunca lo calcula

        BigDecimal guaranteeDepositAmount, // deposito de garantia tal cual de M3; M2 nunca lo calcula

        BigDecimal totalAmount // total vinculante tal cual de M3; M2 no lo recompone
) {
}
