package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselSummary;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface VesselReadPort { // puerto de salida de solo lectura de datos de flota (modulo 1)

    /**
     * Consulta en una sola operacion los datos minimos de varias embarcaciones.
     * La implementacion debe deduplicar, acotar el tiempo total y omitir del mapa
     * las embarcaciones que no se puedan resolver (fail-safe por elemento).
     */
    Map<UUID, VesselSummary> getSummaries(Collection<UUID> vesselIds); // resumen por embarcacion; ids ausentes = no disponible

    /**
     * Consulta la ficha tecnica completa de una embarcacion.
     * Propaga los errores de M1 con el catalogo de ErrorCode para que CU-19
     * pueda distinguir 404 de indisponibilidad.
     */
    VesselDetail getDetail(UUID vesselId); // ficha completa o excepcion de dominio
}
