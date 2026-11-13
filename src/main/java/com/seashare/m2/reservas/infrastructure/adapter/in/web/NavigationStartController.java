package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.StartNavigationCommand;
import com.seashare.m2.reservas.application.dto.StartNavigationResult;
import com.seashare.m2.reservas.application.port.in.StartNavigationUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class NavigationStartController { // CU-06: marcar el inicio de la navegacion

    private final StartNavigationUseCase startNavigationUseCase; // caso de uso de inicio

    public NavigationStartController(StartNavigationUseCase startNavigationUseCase) { // inyeccion del caso de uso
        this.startNavigationUseCase = startNavigationUseCase; // asigna el caso de uso
    }

    @PostMapping("/{reservation_id}/navigation-start")
    public ResponseEntity<NavigationStartResponse> startNavigation(
            CallerIdentity identity, // identidad temporal por headers
            @PathVariable("reservation_id") String reservationId, // reserva recibida como texto
            @RequestBody(required = false) NavigationStartRequest request) { // cuerpo opcional
        UUID id = parseUuid(reservationId); // valida el identificador de la ruta
        ActorType actor = resolveActor(identity.role()); // traduce el rol del llamador

        OffsetDateTime actualDepartureAt = request == null ? null : request.actualDepartureAt(); // salida opcional
        String deliveryNotes = request == null ? null : request.deliveryNotes(); // notas opcionales

        StartNavigationResult result = startNavigationUseCase.execute( // delega sin logica de negocio
                new StartNavigationCommand(id, identity.userId(), actor, actualDepartureAt, deliveryNotes)); // comando de aplicacion

        return ResponseEntity.ok(NavigationStartResponse.from(result)); // 200 con el contrato de respuesta
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
                "Only RENTER or OWNER roles are authorized to start navigation"); // 403
    }
}