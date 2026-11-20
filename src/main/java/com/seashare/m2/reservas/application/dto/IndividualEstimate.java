package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;

public record IndividualEstimate( // estimacion individual devuelta por modulo 3, monto opaco sin aritmetica

        BigDecimal estimatedTotal, // monto tal cual de M3; M2 nunca lo calcula ni redondea

        String warning // texto literal de M3; null si no viene y no es error
) {
}
