package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.VesselSearchCriteria;
import com.seashare.m2.reservas.application.dto.VesselSearchResult;

public interface VesselSearchUseCase { // CU-01: buscar embarcaciones disponibles

    VesselSearchResult search(VesselSearchCriteria criteria, String role); // catalogo filtrado y cotizado en lote
}
