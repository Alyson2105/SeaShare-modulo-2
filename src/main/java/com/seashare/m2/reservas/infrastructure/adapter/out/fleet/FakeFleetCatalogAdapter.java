package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.CatalogVessel;
import com.seashare.m2.reservas.application.dto.VesselCatalogPage;
import com.seashare.m2.reservas.application.port.out.FleetCatalogPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 1.
@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeFleetCatalogAdapter implements FleetCatalogPort { // catalogo simulado y determinista para desarrollo

    static final int CATALOG_SIZE = 25; // suficiente para ejercitar la paginacion de 20

    private static final List<String> TYPES = List.of("MOTORBOAT", "SAILBOAT", "YACHT", "CATAMARAN"); // categorias del contrato

    private static final List<CatalogVessel> CATALOG = buildCatalog(); // catalogo fijo en memoria

    private static List<CatalogVessel> buildCatalog() { // genera el catalogo una sola vez
        List<CatalogVessel> vessels = new ArrayList<>();
        for (int i = 0; i < CATALOG_SIZE; i++) {
            UUID id = UUID.nameUUIDFromBytes(("vessel-demo-" + i).getBytes(StandardCharsets.UTF_8)); // id estable
            String type = TYPES.get(i % TYPES.size()); // rota categorias
            vessels.add(new CatalogVessel(
                    id,
                    "Embarcacion demo " + String.format("%02d", i + 1),
                    type,
                    "https://cdn.seashare.local/demo/" + (i + 1) + ".jpg",
                    i % 3 == 2 ? "Isla demo " + (i + 1) : "Marina demo",
                    11.2443 + (i * 0.001),
                    -74.2125 - (i * 0.001),
                    "America/Bogota",
                    8 + (i % 9),
                    i % 4 != 1));
        }
        return List.copyOf(vessels); // inmutable
    }

    @Override
    public VesselCatalogPage search(String query, int page, int size) { // pagina y filtra por texto
        List<CatalogVessel> filtered = CATALOG.stream()
                .filter(vessel -> matches(vessel, query))
                .toList();
        int from = Math.min((page - 1) * size, filtered.size()); // inicio de la pagina
        int to = Math.min(from + size, filtered.size()); // fin exclusivo
        List<CatalogVessel> slice = filtered.subList(from, to); // lote visible
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new VesselCatalogPage(List.copyOf(slice), page, size, filtered.size(), totalPages);
    }

    private boolean matches(CatalogVessel vessel, String query) { // busqueda textual por nombre o puerto
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return vessel.name().toLowerCase(Locale.ROOT).contains(needle)
                || vessel.portName().toLowerCase(Locale.ROOT).contains(needle);
    }
}
