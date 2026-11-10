package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationListFilter;
import com.seashare.m2.reservas.application.dto.ReservationPage;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.port.in.ReservationQueryUseCase;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservationQueryService implements ReservationQueryUseCase { // servicio de aplicacion para consulta paginada y detalle de reservas

    private final ReservationRepositoryPort queryPort; // puerto de salida para ejecucion de consultas

    private final VesselReadPort vesselReadPort; // puerto de solo lectura de datos de flota (CU-20 y CU-21)

    public ReservationQueryService(ReservationRepositoryPort queryPort, VesselReadPort vesselReadPort) { // inyeccion de puertos de consulta
        this.queryPort = queryPort; // asigna puerto de consulta
        this.vesselReadPort = vesselReadPort; // asigna lectura de flota
    }

    @Override
    public ReservationViewPage findReservations(UUID userId, String role, int page, int limit) { // consulta sin filtro explicito
        return findReservations(userId, role, page, limit, ReservationListFilter.ALL); // aplica el universo completo
    }

    @Override
    public ReservationViewPage findReservations(UUID userId,
                                                String role,
                                                int page,
                                                int limit,
                                                ReservationListFilter filter) { // consulta reservas propias con paginacion y filtro
        validateRole(role); // valida que el rol tenga permisos de consulta
        validatePagination(page, limit); // valida limites de paginacion permitidos

        ReservationListFilter effectiveFilter = filter == null ? ReservationListFilter.ALL : filter; // filtro por defecto
        Set<ReservationStatus> statuses = effectiveFilter.statuses(); // estados del filtro

        ReservationPage pageResult;
        if ("RENTER".equalsIgnoreCase(role.trim())) { // rol arrendatario consulta reservas donde es titular
            pageResult = queryPort.findByRenter(userId, page, limit, statuses); // pagina filtrada del arrendatario
        } else {
            pageResult = queryPort.findByOwner(userId, page, limit, statuses); // pagina filtrada del propietario
        }

        List<ReservationView> items = pageResult.items().stream().map(ReservationView::from).toList(); // proyecciones de la pagina
        Map<UUID, VesselSummary> vessels = resolveVessels(items); // datos de flota con fail-safe por elemento
        return new ReservationViewPage(items, pageResult.totalItems(), effectiveFilter,
                role.trim().toUpperCase(Locale.ROOT), page, limit, vessels); // resultado enriquecido
    }

    @Override
    public ReservationView getReservationDetail(UUID reservationId, UUID userId, String role) { // consulta detalle de reserva propia
        validateRole(role); // valida que el rol tenga permisos de consulta

        Optional<Reservation> reservationOpt = queryPort.findById(reservationId); // busca reserva por su identificador unico
        if (reservationOpt.isEmpty()) { // comprueba si la reserva existe
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"); // rechaza reserva no encontrada
        }
        Reservation reservation = reservationOpt.get();

        boolean owned = false;
        if ("RENTER".equalsIgnoreCase(role.trim())) {
            owned = reservation.isRenter(userId);
        } else if ("OWNER".equalsIgnoreCase(role.trim())) {
            owned = reservation.isOwner(userId);
        }
        if (!owned) { // valida pertenencia de la reserva segun rol
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED, "Reservation does not belong to user"); // rechaza acceso no autorizado
        }

        return ReservationView.from(reservation); // retorna proyeccion inmutable de la reserva
    }

    private void validatePagination(int page, int limit) { // valida page >= 1 y 1 <= limit <= 50
        if (page < 1 || limit < 1 || limit > 50) { // limites fuera de rango
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "Page must be greater than or equal to 1 and limit between 1 and 50"); // rechaza parametros invalidos
        }
    }

    private Map<UUID, VesselSummary> resolveVessels(List<ReservationView> items) { // resuelve resumenes de flota deduplicados
        Set<UUID> vesselIds = new LinkedHashSet<>(); // ids distintos de la pagina
        for (ReservationView item : items) { // recorre la pagina
            if (item.vesselId() != null) { // solo ids utiles
                vesselIds.add(item.vesselId()); // deduplica
            }
        }
        if (vesselIds.isEmpty()) { // sin embarcaciones no hay llamada remota
            return Map.of(); // retorno inmediato
        }
        try {
            Map<UUID, VesselSummary> summaries = vesselReadPort.getSummaries(vesselIds); // consulta agrupada con presupuesto
            return summaries == null ? Map.of() : summaries; // tolera implementaciones nulas
        } catch (RuntimeException e) { // la lista nunca se cae por M1 (D5)
            return Map.of(); // datos de flota no disponibles
        }
    }

    private void validateRole(String role) { // valida pertenencia de rol segun especificacion
        if (role == null || (!"RENTER".equalsIgnoreCase(role.trim()) && !"OWNER".equalsIgnoreCase(role.trim()))) { // restringe a roles de negocio validos
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "User role is not authorized to query reservations"); // rechaza rol no autorizado
        }
    }
}
