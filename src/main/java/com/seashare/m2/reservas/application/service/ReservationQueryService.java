package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.ReservationListFilter;
import com.seashare.m2.reservas.application.dto.ReservationPage;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.port.in.ReservationQueryUseCase;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    private final ClockPort clockPort; // reloj unico para contadores y flags temporales

    public ReservationQueryService(ReservationRepositoryPort queryPort,
                                   VesselReadPort vesselReadPort,
                                   ClockPort clockPort) { // inyeccion de puertos de consulta
        this.queryPort = queryPort; // asigna puerto de consulta
        this.vesselReadPort = vesselReadPort; // asigna lectura de flota
        this.clockPort = clockPort; // asigna reloj
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
    public ReservationView getReservationDetail(UUID reservationId, UUID userId, String role) { // proyeccion basica del detalle
        return ReservationView.from(requireOwnedReservation(reservationId, userId, role)); // valida pertenencia y proyecta
    }

    @Override
    public ReservationDetailView getReservationDetailView(UUID reservationId, UUID userId, String role) { // detalle completo de CU-21
        ReservationView view = ReservationView.from(requireOwnedReservation(reservationId, userId, role)); // valida pertenencia
        ZoneId zone = HarborTime.zoneOf(view.harborTimezone()); // zona del puerto
        OffsetDateTime scheduledDeparture = HarborTime.at(view.startDate(), view.departureTime(), zone); // zarpe pactado
        OffsetDateTime scheduledArrival = HarborTime.at(view.endDate(), view.departureTime(), zone); // llegada pactada (D7)
        OffsetDateTime actualDeparture = toOffset(view.actualDepartureAt(), zone); // zarpe real
        OffsetDateTime actualArrival = toOffset(view.actualArrivalAt(), zone); // llegada real
        Instant now = clockPort.now(); // instante unico de la consulta

        VesselDetail vessel = resolveVesselDetail(view); // ficha con degradacion si M1 falla
        long previousTrips = queryPort.countCompletedByRenterExcluding(view.renterId(), view.reservationId()); // D8: viajes completados en M2

        ReservationDetailView.Itinerary itinerary = new ReservationDetailView.Itinerary(
                scheduledDeparture,
                scheduledArrival,
                actualDeparture,
                actualArrival,
                new ReservationDetailView.DeparturePort(
                        vessel.portName(),
                        vessel.portCity(),
                        view.harborTimezone() != null ? view.harborTimezone() : vessel.timezone()),
                HarborTime.durationDays(view.startDate(), view.endDate()),
                HarborTime.durationNights(view.startDate(), view.endDate()),
                view.passengers());

        ReservationDetailView.Party renter = new ReservationDetailView.Party(
                view.renterId(), view.holderName(), previousTrips); // D9: nombre del titular persistido
        ReservationDetailView.Party owner = new ReservationDetailView.Party(
                view.ownerId(), vessel.ownerName(), null); // el conteo solo aplica al arrendatario

        return new ReservationDetailView(
                view.reservationId(),
                view.reservationCode(),
                view.status(),
                view.cancellationSubtype(),
                vessel,
                itinerary,
                renter,
                owner,
                ReservationDetailPolicy.paymentSummary(view), // desglose adaptado al estado
                ReservationDetailPolicy.actions(view, role, scheduledDeparture, now), // flags por rol/estado
                ReservationDetailPolicy.timeIndicators(view, scheduledDeparture, now), // contadores
                ReservationDetailPolicy.banner(view, actualDeparture), // banner contextual
                view.createdAt().atZone(zone).toOffsetDateTime());
    }

    private Reservation requireOwnedReservation(UUID reservationId, UUID userId, String role) { // valida existencia y pertenencia
        validateRole(role); // valida que el rol tenga permisos de consulta
        Reservation reservation = queryPort.findById(reservationId) // busca la reserva
                .orElseThrow(() -> new ReservationRuleException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"));
        boolean owned = "RENTER".equalsIgnoreCase(role.trim())
                ? reservation.isRenter(userId)
                : reservation.isOwner(userId); // pertenencia segun rol
        if (!owned) { // acceso de terceros
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED, "Reservation does not belong to user");
        }
        return reservation; // reserva autorizada
    }

    private VesselDetail resolveVesselDetail(ReservationView view) { // ficha completa con fail-safe
        try {
            VesselDetail detail = vesselReadPort.getDetail(view.vesselId()); // consulta estricta
            if (detail != null) {
                return detail; // ficha disponible
            }
        } catch (RuntimeException e) {
            // M1 no disponible: se intenta el resumen y, si tambien falla, se devuelve la ficha minima
        }
        try {
            Map<UUID, VesselSummary> summaries = vesselReadPort.getSummaries(Set.of(view.vesselId())); // resumen como respaldo
            VesselSummary summary = summaries == null ? null : summaries.get(view.vesselId());
            if (summary != null) {
                return new VesselDetail(view.vesselId(), summary.name(), null, summary.imageUrl(),
                        summary.portName(), null, view.harborTimezone(), List.of(),
                        summary.ownerId(), summary.ownerName()); // ficha parcial
            }
        } catch (RuntimeException e) {
            // sin datos de flota: se devuelve la ficha minima
        }
        return new VesselDetail(view.vesselId(), null, null, null, null, null,
                view.harborTimezone(), List.of(), view.ownerId(), null); // ficha minima con el id persistido
    }

    private static OffsetDateTime toOffset(Instant instant, ZoneId zone) { // convierte instante opcional a offset del puerto
        return instant == null ? null : instant.atZone(zone).toOffsetDateTime();
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
