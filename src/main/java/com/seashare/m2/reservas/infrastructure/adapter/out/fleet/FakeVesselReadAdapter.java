package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 1.
@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeVesselReadAdapter implements VesselReadPort { // resumen simulado de embarcaciones para desarrollo

    @Override
    public Map<UUID, VesselSummary> getSummaries(Collection<UUID> vesselIds) { // genera un resumen deterministico por id
        Map<UUID, VesselSummary> summaries = new LinkedHashMap<>(); // preserva orden de entrada
        if (vesselIds == null) { // tolera coleccion nula
            return Map.of(); // sin datos
        }
        for (UUID vesselId : vesselIds) { // recorre ids
            if (vesselId == null || summaries.containsKey(vesselId)) { // ignora nulos y duplicados
                continue; // siguiente id
            }
            String shortId = vesselId.toString().substring(0, 8); // fragmento estable para el nombre
            UUID ownerId = UUID.nameUUIDFromBytes(vesselId.toString().getBytes(StandardCharsets.UTF_8)); // propietario deterministico
            summaries.put(vesselId, new VesselSummary( // arma el resumen de prueba
                    vesselId,
                    "Embarcacion demo " + shortId,
                    null,
                    "Marina demo",
                    ownerId,
                    "Propietario demo"));
        }
        return Map.copyOf(summaries); // mapa inmutable
    }

    @Override
    public VesselDetail getDetail(UUID vesselId) { // ficha simulada determinista
        String shortId = vesselId == null ? "desconocida" : vesselId.toString().substring(0, 8); // fragmento estable
        UUID ownerId = vesselId == null ? null : UUID.nameUUIDFromBytes(vesselId.toString().getBytes(StandardCharsets.UTF_8)); // propietario deterministico
        return new VesselDetail(
                vesselId,
                "Embarcacion demo " + shortId,
                "REG-DEMO-" + shortId,
                "CATAMARAN",
                null,
                12,
                48.5,
                3,
                true,
                "Marina demo",
                null,
                11.2443,
                -74.2125,
                "America/Bogota",
                List.of("GPS Nautico", "Chalecos salvavidas"),
                ownerId,
                "Propietario demo",
                true);
    }
}
