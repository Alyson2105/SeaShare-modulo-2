package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.VesselCatalogPage;

public interface FleetCatalogPort { // CU-01: catalogo paginado de modulo 1 (CU-09 operacion en lote)

    VesselCatalogPage search(String query, int page, int size); // consulta una pagina del catalogo
}
