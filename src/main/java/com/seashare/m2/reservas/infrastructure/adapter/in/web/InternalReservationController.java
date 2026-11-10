package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.application.port.in.GetInternalReservationUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class InternalReservationController { // endpoint interno de solo lectura para Modulo 3 (CU-15)

    private final GetInternalReservationUseCase getInternalReservation; // caso de uso de lectura

    public InternalReservationController(GetInternalReservationUseCase getInternalReservation) { // constructor con dependencia
        this.getInternalReservation = getInternalReservation; // asigna caso de uso
    }

    @GetMapping("/api/v1/internal/reservations/{reservationId}")
    public InternalReservationResponse getReservation(
            CallerIdentity identity, // identidad ya validada por el resolver de headers
            @PathVariable("reservationId") String reservationId // identificador recibido como texto
    ) { // consulta operativa de la reserva sin efectos secundarios
        UUID id; // identificador validado
        try {
            id = UUID.fromString(reservationId.trim()); // parsea el formato uuid
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in reservation_id"); // rechaza formato invalido
        }

        InternalReservationView view = getInternalReservation.execute(id); // ejecuta la consulta de solo lectura
        return InternalReservationResponse.from(view); // transforma la vista a respuesta snake_case
    }
}
