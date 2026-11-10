package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.dto.VesselSummary;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record ReservationListResponse( // envoltorio de respuesta del listado de CU-20

        @JsonProperty("pagination")
        PaginationResponse pagination, // metadatos de paginacion

        @JsonProperty("applied_filter")
        String appliedFilter, // filtro aplicado

        @JsonProperty("user_role")
        String userRole, // rol con el que se resolvio la consulta

        @JsonProperty("view_title")
        String viewTitle, // titulo semantico segun rol

        @JsonProperty("reservations")
        List<ReservationListItemResponse> reservations // pagina actual
) {

    public record PaginationResponse(
            @JsonProperty("page")
            int page, // pagina solicitada

            @JsonProperty("page_size")
            int pageSize, // tamano de pagina

            @JsonProperty("total_items")
            long totalItems, // total de elementos del filtro

            @JsonProperty("total_pages")
            long totalPages // total de paginas
    ) {
    }

    public static ReservationListResponse from(ReservationViewPage page) { // transforma la vista de aplicacion
        Map<UUID, VesselSummary> vessels = page.vessels() == null ? Map.of() : page.vessels(); // resumenes disponibles
        List<ReservationListItemResponse> items = page.items().stream()
                .map(view -> ReservationListItemResponse.from(view, vessels.get(view.vesselId()), page.role()))
                .toList(); // tarjetas de la pagina
        long totalItems = page.totalItems(); // total con filtro aplicado
        long totalPages = totalItems == 0 ? 0 : (totalItems + page.limit() - 1) / page.limit(); // paginas totales
        return new ReservationListResponse(
                new PaginationResponse(page.page(), page.limit(), totalItems, totalPages),
                page.filter().externalValue(),
                page.role().toUpperCase(Locale.ROOT),
                "OWNER".equalsIgnoreCase(page.role()) ? "Reservas recibidas" : "Mis reservas",
                items);
    }
}
