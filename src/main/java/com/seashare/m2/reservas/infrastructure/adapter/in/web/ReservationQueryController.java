package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.ReservationListFilter;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.port.in.ReservationQueryUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationQueryController { // controlador rest para consultas paginadas y detalle de reservas

    private final ReservationQueryUseCase queryService; // caso de uso para resolucion de consultas

    public ReservationQueryController(ReservationQueryUseCase queryService) { // inyeccion del caso de uso de consultas
        this.queryService = queryService; // asigna servicio de consultas
    }

    @GetMapping
    public ReservationListResponse listReservations(
            CallerIdentity identity, // identidad extraida de encabezados http
            @RequestParam(name = "page", defaultValue = "1") int page, // pagina solicitada por defecto uno
            @RequestParam(name = "limit", defaultValue = "10") int limit, // tamano solicitado por defecto diez
            @RequestParam(name = "filter", defaultValue = "all") String filter // pestana all/history/cancelled
    ) { // endpoint para listar reservas propias de forma paginada
        ReservationListFilter parsedFilter = ReservationListFilter.from(filter); // valida y traduce el filtro
        ReservationViewPage result = queryService.findReservations(
                identity.userId(), identity.role(), page, limit, parsedFilter); // ejecuta consulta con filtro
        return ReservationListResponse.from(result); // construye el payload del contrato CU-20
    }

    @GetMapping("/{id}")
    public ReservationDetailResponse getReservation(
            CallerIdentity identity, // identidad extraida de encabezados http
            @PathVariable("id") String id // identificador recibido como texto en la ruta
    ) { // endpoint para consultar el detalle de una reserva especifica
        UUID reservationId; // identificador unico validado
        try {
            reservationId = UUID.fromString(id.trim()); // parsea y valida formato uuid
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in reservation path"); // rechaza ruta con formato invalido
        }

        ReservationDetailView view = queryService.getReservationDetailView(
                reservationId, identity.userId(), identity.role()); // consulta detalle completo
        return ReservationDetailResponse.from(view); // transforma vista a respuesta web
    }
}
