package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservationQueryService { // servicio de aplicacion para consulta paginada y detalle de reservas

    private final ReservationRepositoryPort queryPort; // puerto de salida para ejecucion de consultas

    public ReservationQueryService(ReservationRepositoryPort queryPort) { // inyeccion del puerto de consulta
        this.queryPort = queryPort; // asigna puerto de consulta
    }

    public ReservationViewPage findReservations(UUID userId, String role, int page, int limit) { // consulta reservas propias con paginacion
        validateRole(role); // valida que el rol tenga permisos de consulta

        if (page < 1 || limit < 1 || limit > 50) { // valida limites de paginacion permitidos
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "Page must be greater than or equal to 1 and limit between 1 and 50"); // rechaza parametros fuera de rango
        }

        ReservationRepositoryPort.ReservationPage pageResult;
        if ("RENTER".equalsIgnoreCase(role)) { // rol arrendatario consulta reservas donde es titular
            pageResult = queryPort.findByRenter(userId, page, limit); // retorna pagina de reservas del arrendatario
        } else {
            pageResult = queryPort.findByOwner(userId, page, limit); // retorna pagina de reservas del propietario
        }
        java.util.List<ReservationView> items = pageResult.items().stream().map(ReservationView::from).toList();
        return new ReservationViewPage(items, pageResult.totalItems());
    }

    public ReservationView getReservationDetail(UUID reservationId, UUID userId, String role) { // consulta detalle de reserva propia
        validateRole(role); // valida que el rol tenga permisos de consulta

        Optional<Reservation> reservationOpt = queryPort.findById(reservationId); // busca reserva por su identificador unico
        if (reservationOpt.isEmpty()) { // comprueba si la reserva existe
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"); // rechaza reserva no encontrada
        }
        Reservation reservation = reservationOpt.get();

        boolean owned = false;
        if ("RENTER".equalsIgnoreCase(role)) {
            owned = reservation.isRenter(userId);
        } else if ("OWNER".equalsIgnoreCase(role)) {
            owned = reservation.isOwner(userId);
        }
        if (!owned) { // valida pertenencia de la reserva segun rol
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED, "Reservation does not belong to user"); // rechaza acceso no autorizado
        }

        return ReservationView.from(reservation); // retorna proyeccion inmutable de la reserva
    }

    private void validateRole(String role) { // valida pertenencia de rol segun especificacion
        if (role == null || (!"RENTER".equalsIgnoreCase(role.trim()) && !"OWNER".equalsIgnoreCase(role.trim()))) { // restringe a roles de negocio validos
            // TODO: solo roles RENTER y OWNER pueden consultar reservas segun especificacion
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "User role is not authorized to query reservations"); // rechaza rol no autorizado
        }
    }
}
