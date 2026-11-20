package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StartPaymentResponse( // contrato de respuesta de CU-03

        @JsonProperty("reservation_id")
        UUID reservationId, // reserva transicionada

        @JsonProperty("status")
        ReservationStatus status, // PENDING_PAYMENT

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion retenida

        @JsonProperty("total_calculation")
        TotalCalculationResponse totalCalculation, // desglose literal de M3

        @JsonProperty("payment")
        PaymentResponse payment, // estado del cobro

        @JsonProperty("ttl_remaining_seconds")
        long ttlRemainingSeconds, // segundos restantes del TTL original

        @JsonProperty("ttl_expires_at")
        OffsetDateTime ttlExpiresAt // vencimiento original
) {

    static final String GUARANTEE_MESSAGE =
            "El depósito se reembolsa completo si el barco se devuelve sin daños"; // texto constante de M2

    public record TotalCalculationResponse(
            @JsonProperty("currency")
            String currency, // COP

            @JsonProperty("total_amount")
            BigDecimal totalAmount, // literal de M3

            @JsonProperty("breakdown")
            BreakdownResponse breakdown, // rubros literales

            @JsonProperty("guarantee_message")
            String guaranteeMessage // texto informativo
    ) {
    }

    public record BreakdownResponse(
            @JsonProperty("rental_amount")
            BigDecimal rentalAmount, // alquiler

            @JsonProperty("insurance_amount")
            BigDecimal insuranceAmount, // seguro

            @JsonProperty("guarantee_deposit_amount")
            BigDecimal guaranteeDepositAmount // deposito
    ) {
    }

    public record PaymentResponse(
            @JsonProperty("charge_status")
            String chargeStatus, // IN_PROCESS

            @JsonProperty("attempt")
            int attempt, // numero de intento

            @JsonProperty("confirmation_mode")
            String confirmationMode // QUERY
    ) {
    }

    public static StartPaymentResponse from(StartPaymentResult result) { // transforma el resultado
        CalculatedValue calculation = result.calculation();
        return new StartPaymentResponse(
                result.reservationId(),
                result.status(),
                result.vesselId(),
                new TotalCalculationResponse(
                        "COP",
                        calculation.totalAmount(),
                        new BreakdownResponse(
                                calculation.rentalAmount(),
                                calculation.insuranceAmount(),
                                calculation.guaranteeDepositAmount()),
                        GUARANTEE_MESSAGE),
                new PaymentResponse("IN_PROCESS", result.attempt(), "QUERY"),
                result.ttlRemainingSeconds(),
                result.ttlExpiresAt());
    }
}
