package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record VesselSearchResult( // resultado de CU-01, ya filtrado y cotizado
        int page, // pagina solicitada
        int pageSize, // tamano fijo del lote
        long totalItems, // total informado por el catalogo
        int totalPages, // total de paginas del catalogo
        String contextLabel, // etiqueta descriptiva de los resultados
        Map<UUID, BigDecimal> estimates, // cotizacion literal por embarcacion
        java.util.List<CatalogVessel> vessels // embarcaciones visibles de la pagina
) {
}
