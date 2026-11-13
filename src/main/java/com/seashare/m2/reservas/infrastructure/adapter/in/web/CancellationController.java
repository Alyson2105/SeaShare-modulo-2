package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.CancellationResult;
import com.seashare.m2.reservas.application.dto.RequestCancellationCommand;
import com.seashare.m2.reservas.application.port.in.RequestCancellationUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class CancellationController { // CU-04: solicitar la cancelacion de una reserva

    private final RequestCancellationUseCase requestCancellationUseCase; // caso de uso de cancelacion

    public CancellationController(RequestCancellationUseCase requestCancellationUseCase) { // inyeccion del caso de uso
        this.requestCancellationUseCase = requestCancellationUseCase; // asigna el caso de uso
    }

    @PostMapping("/{reservation_id}/cancellation")
    public ResponseEntity<CancellationResponse> cancel(
            CallerIdentity identity, // identidad temporal por headers
            @PathVariable("reservation_id") String reservationId, // reserva recibida como texto
            @Valid @RequestBody(required = false) CancellationRequest request) { // cuerpo opcional
        UUID id = parseUuid(reservationId); // valida el identificador de la ruta
        ActorType actor = resolveActor(identity.role()); // traduce el rol del llamador

        String reason = request == null ? null : request.reason(); // motivo opcional
        String justification = request == null ? null : request.justification(); // justificacion opcional

        CancellationResult result = requestCancellationUseCase.execute( // delega sin logica de negocio
                new RequestCancellationCommand(id, identity.userId(), actor, reason, justification)); // comando de aplicacion

        return ResponseEntity.ok(CancellationResponse.from(result)); // 200 con el contrato de respuesta
    }

    private UUID parseUuid(String value) { // parsea el identificador de ruta
        try {
            return UUID.fromString(value == null ? "" : value.trim()); // formato uuid valido
        } catch (IllegalArgumentException e) { // formato invalido
            throw new ReservationRuleException(ErrorCode.INVALID_UUID,
                    "Invalid UUID format in reservation path"); // 400 INVALID_UUID
        }
    }

    private ActorType resolveActor(String role) { // traduce el rol sin distinguir mayusculas
        if ("RENTER".equalsIgnoreCase(role)) { // arrendatario
            return ActorType.RENTER; // actor arrendatario
        }
        if ("OWNER".equalsIgnoreCase(role)) { // propietario
            return ActorType.OWNER; // actor propietario
        }
        throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                "Only RENTER or OWNER roles are authorized to cancel reservations"); // 403
    }
}