package com.seashare.m2.reservas.application.dto;

import java.time.LocalDate;

public record VesselSearchCriteria( // criterios opcionales de busqueda (CU-01 FR-001)
        LocalDate startDate, // fecha inicial deseada
        LocalDate endDate, // fecha final deseada
        Integer passengers, // pasajeros requeridos
        String type, // tipo de embarcacion, ya validado
        String text, // busqueda textual libre
        int page // pagina solicitada, base 1
) {
}
