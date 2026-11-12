package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.CatalogVessel;
import com.seashare.m2.reservas.application.dto.VesselSearchResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VesselSearchResponse( // contrato de respuesta de CU-01

        @JsonProperty("pagination")
        PaginationResponse pagination, // metadatos de paginacion

        @JsonProperty("context_label")
        String contextLabel, // etiqueta descriptiva de los resultados

        @JsonProperty("vessels")
        List<VesselCardResponse> vessels // tarjetas del catalogo
) {

    public record PaginationResponse(
            @JsonProperty("page")
            int page, // pagina solicitada

            @JsonProperty("page_size")
            int pageSize, // tamano fijo de lote

            @JsonProperty("total_items")
            long totalItems, // total del catalogo

            @JsonProperty("total_pages")
            int totalPages // total de paginas
    ) {
    }

    public record VesselCardResponse(
            @JsonProperty("vessel_id")
            UUID vesselId, // identificador para extender a CU-19

            @JsonProperty("name")
            String name, // nombre comercial

            @JsonProperty("type")
            String type, // categoria

            @JsonProperty("photo_url")
            String photoUrl, // imagen de la tarjeta

            @JsonProperty("port")
            PortResponse port, // puerto base

            @JsonProperty("max_capacity")
            int maxCapacity, // pasajeros maximos

            @JsonProperty("captain_included")
            boolean captainIncluded, // viaje con capitan

            @JsonProperty("quotation_available")
            boolean quotationAvailable, // false si M3 no pudo cotizar

            @JsonProperty("estimated_total")
            BigDecimal estimatedTotal, // monto literal de M3 o null

            @JsonProperty("currency")
            String currency // COP fijo
    ) {
    }

    public record PortResponse(
            @JsonProperty("name")
            String name, // marina o puerto

            @JsonProperty("latitude")
            Double latitude, // coordenada

            @JsonProperty("longitude")
            Double longitude, // coordenada

            @JsonProperty("timezone")
            String timezone // zona IANA
    ) {
    }

    public static VesselSearchResponse from(VesselSearchResult result) { // transforma el resultado de aplicacion
        List<VesselCardResponse> cards = result.vessels().stream()
                .map(vessel -> toCard(vessel, result.estimates().get(vessel.vesselId())))
                .toList();
        return new VesselSearchResponse(
                new PaginationResponse(result.page(), result.pageSize(), result.totalItems(), result.totalPages()),
                result.contextLabel(),
                cards);
    }

    private static VesselCardResponse toCard(CatalogVessel vessel, BigDecimal estimate) { // arma una tarjeta
        return new VesselCardResponse(
                vessel.vesselId(),
                vessel.name(),
                vessel.type(),
                vessel.photoUrl(),
                new PortResponse(vessel.portName(), vessel.latitude(), vessel.longitude(), vessel.timezone()),
                vessel.maxCapacity(),
                vessel.captainIncluded(),
                estimate != null, // SC-001: cotizacion disponible salvo caida
                estimate, // literal de M3, sin calculo
                "COP"); // constante de plataforma
    }
}
