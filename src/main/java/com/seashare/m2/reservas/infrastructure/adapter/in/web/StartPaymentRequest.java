package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StartPaymentRequest( // cuerpo de CU-03

        @JsonProperty("accepts_cancellation_policy")
        Boolean acceptsCancellationPolicy, // FR-014: obligatorio y true

        @JsonProperty("payment")
        PaymentData payment // datos del medio de pago
) {

    public record PaymentData(
            @JsonProperty("payment_token_ref")
            String paymentTokenRef, // token de un solo uso del frontend

            @JsonProperty("payment_method_type")
            String paymentMethodType, // ej. CREDIT_CARD

            @JsonProperty("payment_metadata")
            PaymentMetadata paymentMetadata // datos exigidos por M3
    ) {
    }

    public record PaymentMetadata(
            @JsonProperty("payer_email")
            String payerEmail, // obligatorio

            @JsonProperty("payment_method_id")
            String paymentMethodId, // obligatorio

            @JsonProperty("installments")
            Integer installments, // obligatorio

            @JsonProperty("last_four")
            String lastFour // opcional e informativo
    ) {
    }
}
