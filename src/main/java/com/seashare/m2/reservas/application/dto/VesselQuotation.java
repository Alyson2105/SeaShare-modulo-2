package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VesselQuotation( // cotizacion individual de CU-11 expuesta por CU-19

        UUID vesselId, // embarcacion cotizada

        LocalDate startDate, // fecha de inicio

        LocalDate endDate, // fecha de fin

        int totalDays, // dias inclusivos del rango (aritmetica de calendario, no monetaria)

        int passengers, // pasajeros cotizados

        BigDecimal totalAmount, // monto literal de M3

        String currency, // moneda constante de la plataforma

        String warning // advertencia literal de M3
) {
}
