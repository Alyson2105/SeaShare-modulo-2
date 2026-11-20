package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record StartPaymentRequest( // cuerpo de CU-03

        @JsonProperty("accepts_cancellation_policy")
        Boolean acceptsCancellationPolicy, // FR-014: obligatorio y true

        @Valid
        @JsonProperty("payment")
        PaymentData payment // datos del medio de pago
) {

    public record PaymentData(
            @Size(max = 255, message = "payment_token_ref accepts at most 255 characters")
            @JsonProperty("payment_token_ref")
            String paymentTokenRef, // token de un solo uso del frontend

            @Size(max = 50, message = "payment_method_type accepts at most 50 characters")
            @JsonProperty("payment_method_type")
            String paymentMethodType, // ej. CREDIT_CARD

            @Valid
            @JsonProperty("payment_metadata")
            PaymentMetadata paymentMetadata // datos exigidos por M3
    ) {
    }

    public record PaymentMetadata(
            @Size(max = 254, message = "payer_email accepts at most 254 characters")
            @JsonProperty("payer_email")
            String payerEmail, // obligatorio

            @Size(max = 100, message = "payment_method_id accepts at most 100 characters")
            @JsonProperty("payment_method_id")
            String paymentMethodId, // obligatorio

            @JsonProperty("installments")
            Integer installments, // obligatorio

            @Size(max = 4, message = "last_four accepts at most 4 characters")
            @JsonProperty("last_four")
            String lastFour // opcional e informativo
    ) {
    }
}
