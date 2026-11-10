package com.seashare.m2.reservas.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReservationViewPage(
        List<ReservationView> items, // reservas de la pagina
        long totalItems, // total de reservas que cumplen el filtro
        ReservationListFilter filter, // filtro aplicado
        String role, // rol con el que se resolvio la consulta
        int page, // pagina solicitada
        int limit, // tamano de pagina
        Map<UUID, VesselSummary> vessels // resumen de flota por embarcacion; ids ausentes = no disponible
) {
}
