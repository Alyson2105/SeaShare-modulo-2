package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.BatchEstimateResult;
import com.seashare.m2.reservas.application.dto.CatalogVessel;
import com.seashare.m2.reservas.application.dto.VesselCatalogPage;
import com.seashare.m2.reservas.application.dto.VesselSearchCriteria;
import com.seashare.m2.reservas.application.dto.VesselSearchResult;
import com.seashare.m2.reservas.application.port.in.VesselSearchUseCase;
import com.seashare.m2.reservas.application.port.out.BatchEstimatePort;
import com.seashare.m2.reservas.application.port.out.FleetCatalogPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VesselSearchService implements VesselSearchUseCase { // CU-01: catalogo en lote y cotizacion en lote

    static final int PAGE_SIZE = 20; // FR-008: tamano fijo e inmutable

    public static final Set<String> ALLOWED_TYPES = Set.of("MOTORBOAT", "SAILBOAT", "YACHT", "CATAMARAN"); // FR-010

    private static final String RENTER_ROLE = "RENTER"; // perfil autorizado

    private final FleetCatalogPort catalogPort; // catalogo de M1 (CU-09 en lote)

    private final BatchEstimatePort batchEstimatePort; // cotizacion en lote de M3 (CU-11)

    private final ReservationRepositoryPort reservationRepository; // retenciones locales de M2

    public VesselSearchService(FleetCatalogPort catalogPort,
                               BatchEstimatePort batchEstimatePort,
                               ReservationRepositoryPort reservationRepository) {
        this.catalogPort = catalogPort; // asigna catalogo
        this.batchEstimatePort = batchEstimatePort; // asigna cotizacion
        this.reservationRepository = reservationRepository; // asigna reservas
    }

    @Override
    public VesselSearchResult search(VesselSearchCriteria criteria, String role) { // busca, filtra y cotiza
        if (!RENTER_ROLE.equalsIgnoreCase(role)) { // solo el arrendatario explora el catalogo
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Este recurso requiere perfil Arrendatario"); // 403 sin consultar M1 ni M3
        }

        VesselCatalogPage page = catalogPort.search(criteria.text(), criteria.page(), PAGE_SIZE); // FR-002

        List<CatalogVessel> visible = page.vessels().stream() // filtros locales de M2
                .filter(vessel -> matchesType(vessel, criteria.type())) // FR-001: M1 no filtra por tipo
                .filter(vessel -> matchesCapacity(vessel, criteria.passengers())) // FR-001: pasajeros
                .filter(vessel -> isAvailable(vessel, criteria)) // solapamiento contra reservas retenidas
                .toList();

        Map<UUID, BigDecimal> estimates = quote(visible); // FR-003: solo los ids visibles

        return new VesselSearchResult(
                page.page(),
                page.pageSize(),
                page.totalItems(),
                page.totalPages(),
                contextLabel(criteria, visible, estimates),
                estimates,
                visible);
    }

    private Map<UUID, BigDecimal> quote(List<CatalogVessel> vessels) { // cotizacion en lote tolerante a fallos
        if (vessels.isEmpty()) { // sin resultados no se consulta M3
            return Map.of();
        }
        List<UUID> ids = vessels.stream().map(CatalogVessel::vesselId).toList();
        BatchEstimateResult batch = batchEstimatePort.getBatchEstimates(ids); // M3 puede degradar por bloque
        return batch.estimates(); // montos literales; los ausentes quedan como no disponibles
    }

    private boolean matchesType(CatalogVessel vessel, String type) { // filtro por categoria
        return type == null || type.isBlank() || type.equalsIgnoreCase(vessel.type());
    }

    private boolean matchesCapacity(CatalogVessel vessel, Integer passengers) { // filtro por capacidad informada
        return passengers == null || vessel.maxCapacity() >= passengers;
    }

    private boolean isAvailable(CatalogVessel vessel, VesselSearchCriteria criteria) { // disponibilidad local por fechas
        if (criteria.startDate() == null || criteria.endDate() == null) { // sin fechas no se filtra
            return true;
        }
        return !reservationRepository.existsOverlap(
                vessel.vesselId(), criteria.startDate(), criteria.endDate(),
                CreateReservationService.BLOCKING_STATUSES); // PENDING_PAYMENT, RESERVED, IN_NAVIGATION
    }

    private String contextLabel(VesselSearchCriteria criteria,
                                List<CatalogVessel> visible,
                                Map<UUID, BigDecimal> estimates) { // FR-007: contexto dinamico
        if (visible.isEmpty()) { // sin coincidencias
            return "Sin resultados disponibles";
        }
        boolean anyQuote = visible.stream().anyMatch(v -> estimates.containsKey(v.vesselId()));
        if (!anyQuote) { // degradacion total de M3
            return "Catálogo general (Tarifas en actualización)";
        }
        boolean noFilters = (criteria.type() == null || criteria.type().isBlank())
                && (criteria.text() == null || criteria.text().isBlank())
                && criteria.startDate() == null && criteria.endDate() == null;
        return noFilters ? "Populares esta semana" : "Resultados de búsqueda en el Caribe colombiano";
    }
}
