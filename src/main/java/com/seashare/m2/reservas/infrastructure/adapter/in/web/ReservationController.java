package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController { // controlador rest para operaciones sobre reservas

    private final CreateReservationUseCase createReservationUseCase; // caso de uso para creacion de reservas

    public ReservationController(CreateReservationUseCase createReservationUseCase) { // inyeccion del caso de uso
        this.createReservationUseCase = createReservationUseCase; // asigna caso de uso
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            CallerIdentity identity, // identidad extraida antes del cuerpo para validar autenticacion
            @RequestHeader("Idempotency-Key") String idempotencyKey, // clave de idempotencia obligatoria
            @Valid @RequestBody CreateReservationRequest request // cuerpo de solicitud sujeto a validacion de jakarta
    ) { // endpoint principal para formalizar la creacion de la reserva
        if (!"RENTER".equalsIgnoreCase(identity.role())) { // restringe la creacion exclusivamente al rol arrendatario
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "Only RENTER role is authorized to initiate reservations"); // rechaza rol no autorizado
        }

        UUID idempotencyUuid;
        try {
            idempotencyUuid = UUID.fromString(idempotencyKey.trim()); // parsea y valida formato uuid de la clave
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in Idempotency-Key header"); // rechaza formato invalido de clave
        }

        CreateReservationCommand command = new CreateReservationCommand( // construye comando desacoplado para la aplicacion
                identity.userId(), // id del arrendatario
                idempotencyUuid.toString(), // clave de idempotencia normalizada
                request.vesselId(), // embarcacion
                request.startDate(), // fecha de inicio
                request.endDate(), // fecha de fin
                request.passengers(), // pasajeros
                request.holder().fullName(), // nombre titular
                request.holder().phone(), // telefono titular
                request.holder().email(), // correo titular
                LocalTime.parse(request.departureTime()), // hora de salida parseada
                new BigDecimal(request.estimatedTotal()) // monto estimado sin aritmetica
        );

        ReservationView view = createReservationUseCase.execute(command); // delega en el caso de uso
        return ReservationResponse.from(view); // transforma vista de dominio a payload http 201
    }
}
