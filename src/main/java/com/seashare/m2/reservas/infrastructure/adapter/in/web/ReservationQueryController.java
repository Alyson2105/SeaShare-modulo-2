package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.port.out.ReservationQueryPort;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import com.seashare.m2.reservas.application.service.ReservationQueryService;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationQueryController { // controlador rest para consultas paginadas y detalle de reservas

    private final ReservationQueryService queryService; // servicio de aplicacion para resolucion de consultas

    public ReservationQueryController(ReservationQueryService queryService) { // inyeccion del servicio de consultas
        this.queryService = queryService; // asigna servicio de consultas
    }

    public record ReservationListResponse(
            @JsonProperty("items")
            List<ReservationResponse> items, // coleccion de reservas en la pagina actual

            @JsonProperty("page")
            int page, // numero de pagina consultada

            @JsonProperty("limit")
            int limit, // cantidad maxima de elementos por pagina

            @JsonProperty("total_items")
            long totalItems // total general de elementos coincidentes
    ) { // estructura de respuesta para lista paginada en snake_case
    }

    @GetMapping
    public ReservationListResponse listReservations(
            CallerIdentity identity, // identidad extraida de encabezados http
            @RequestParam(name = "page", defaultValue = "1") int page, // pagina solicitada por defecto uno
            @RequestParam(name = "limit", defaultValue = "10") int limit // tamano solicitado por defecto diez
    ) { // endpoint para listar reservas propias de forma paginada
        ReservationQueryPort.ReservationPage result = queryService.findReservations(identity.userId(), identity.role(), page, limit); // ejecuta consulta en servicio
        List<ReservationResponse> items = result.items().stream() // procesa lista de vistas obtenidas
                .map(ReservationResponse::from) // transforma cada vista a respuesta web
                .toList(); // recopila lista inmutable
        return new ReservationListResponse(items, page, limit, result.totalItems()); // construye payload paginado
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(
            CallerIdentity identity, // identidad extraida de encabezados http
            @PathVariable("id") String id // identificador recibido como texto en la ruta
    ) { // endpoint para consultar el detalle de una reserva especifica
        UUID reservationId; // identificador unico validado
        try {
            reservationId = UUID.fromString(id.trim()); // parsea y valida formato uuid
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in reservation path"); // rechaza ruta con formato invalido
        }

        ReservationView view = queryService.getReservationDetail(reservationId, identity.userId(), identity.role()); // consulta detalle en servicio
        return ReservationResponse.from(view); // transforma vista a respuesta web
    }
}
