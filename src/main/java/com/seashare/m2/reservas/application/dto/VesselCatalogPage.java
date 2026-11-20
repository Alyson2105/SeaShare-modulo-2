package com.seashare.m2.reservas.application.dto;

import java.util.List;

public record VesselCatalogPage( // pagina del catalogo devuelta por modulo 1
        List<CatalogVessel> vessels, // embarcaciones de la pagina
        int page, // pagina solicitada (base 1)
        int pageSize, // tamano fijo del lote
        long totalItems, // total informado por modulo 1
        int totalPages // total de paginas informado por modulo 1
) {
}
