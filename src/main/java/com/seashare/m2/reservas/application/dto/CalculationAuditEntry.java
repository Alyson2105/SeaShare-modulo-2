package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CalculationAuditEntry( // asiento inmutable de una solicitud de calculo total (CU-12 FR-010)
        UUID reservationId, // reserva calculada
        String operationId, // identificador de operacion externo; nulo si M3 no lo entrega
        BigDecimal rentalAmount, // alquiler recibido; nulo si el calculo fallo
        BigDecimal insuranceAmount, // seguro recibido; nulo si el calculo fallo
        BigDecimal guaranteeDepositAmount, // deposito recibido; nulo si el calculo fallo
        BigDecimal totalAmount, // total recibido; nulo si el calculo fallo
        String calculationStatus, // SUCCESS o FAILED
        Instant occurredAt // instante de la solicitud
) {
}
