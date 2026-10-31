package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.port.out.ReservationQueryPort;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservationQueryService { // servicio de aplicacion para consulta paginada y detalle de reservas

    private final ReservationQueryPort queryPort; // puerto de salida para ejecucion de consultas

    public ReservationQueryService(ReservationQueryPort queryPort) { // inyeccion del puerto de consulta
        this.queryPort = queryPort; // asigna puerto de consulta
    }

    public ReservationQueryPort.ReservationPage findReservations(UUID userId, String role, int page, int limit) { // consulta reservas propias con paginacion
        validateRole(role); // valida que el rol tenga permisos de consulta

        if (page < 1 || limit < 1 || limit > 50) { // valida limites de paginacion permitidos
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "Page must be greater than or equal to 1 and limit between 1 and 50"); // rechaza parametros fuera de rango
        }

        if ("RENTER".equalsIgnoreCase(role)) { // rol arrendatario consulta reservas donde es titular
            return queryPort.findByRenter(userId, page, limit); // retorna pagina de reservas del arrendatario
        }

        return queryPort.findByOwner(userId, page, limit); // retorna pagina de reservas del propietario
    }

    public ReservationView getReservationDetail(UUID reservationId, UUID userId, String role) { // consulta detalle de reserva propia
        validateRole(role); // valida que el rol tenga permisos de consulta

        Optional<ReservationView> viewOpt = queryPort.findById(reservationId); // busca reserva por su identificador unico
        if (viewOpt.isEmpty()) { // comprueba si la reserva existe
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"); // rechaza reserva no encontrada
        }

        if (!queryPort.isOwnedByUser(reservationId, userId, role)) { // valida pertenencia de la reserva segun rol
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED, "Reservation does not belong to user"); // rechaza acceso no autorizado
        }

        return viewOpt.get(); // retorna proyeccion inmutable de la reserva
    }

    private void validateRole(String role) { // valida pertenencia de rol segun especificacion
        if (role == null || (!"RENTER".equalsIgnoreCase(role.trim()) && !"OWNER".equalsIgnoreCase(role.trim()))) { // restringe a roles de negocio validos
            // TODO: solo roles RENTER y OWNER pueden consultar reservas segun especificacion
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "User role is not authorized to query reservations"); // rechaza rol no autorizado
        }
    }
}
