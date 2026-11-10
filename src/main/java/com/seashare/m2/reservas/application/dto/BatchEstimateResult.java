package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BatchEstimateResult( // resultado de la estimacion en lote de modulo 3 (CU-11)

        Map<UUID, BigDecimal> estimates, // total opaco por embarcacion disponible; sin aritmetica local

        List<UUID> unavailable // embarcaciones sin estimacion: rechazadas, ausentes o bloque fallido
) {
}
