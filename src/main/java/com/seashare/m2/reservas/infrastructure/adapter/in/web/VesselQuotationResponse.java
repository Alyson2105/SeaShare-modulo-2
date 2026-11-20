package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.VesselQuotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VesselQuotationResponse( // cotizacion individual de CU-19 Endpoint 2

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion cotizada

        @JsonProperty("start_date")
        LocalDate startDate, // fecha de inicio

        @JsonProperty("end_date")
        LocalDate endDate, // fecha de fin

        @JsonProperty("total_days")
        int totalDays, // dias inclusivos

        @JsonProperty("passengers")
        int passengers, // pasajeros cotizados

        @JsonProperty("total_amount")
        BigDecimal totalAmount, // monto literal de M3

        @JsonProperty("currency")
        String currency, // moneda constante

        @JsonProperty("warning")
        String warning, // advertencia literal de M3

        @JsonProperty("quotation_id")
        UUID quotationId, // M3 no devuelve identificador de cotizacion

        @JsonProperty("quotation_expires_at")
        OffsetDateTime quotationExpiresAt // M3 no devuelve expiracion; la vigencia la define CU-19
) {

    public static VesselQuotationResponse from(VesselQuotation quotation) { // transforma la cotizacion de aplicacion
        return new VesselQuotationResponse(
                quotation.vesselId(),
                quotation.startDate(),
                quotation.endDate(),
                quotation.totalDays(),
                quotation.passengers(),
                quotation.totalAmount(),
                quotation.currency(),
                quotation.warning(),
                null, // no inventado: M3 no lo entrega
                null); // no inventado: M3 no lo entrega
    }
}
