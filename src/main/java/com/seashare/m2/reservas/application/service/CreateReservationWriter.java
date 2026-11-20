package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.InitiateReservationCommand;
import com.seashare.m2.reservas.application.port.in.InitiateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Escritura transaccional de la creacion de reservas (CU-02 + T-24).
 *
 * <p>Se separa de {@link CreateReservationService} para que las llamadas a Modulo 1 ocurran
 * fuera de la transaccion: aqui solo hay reclamacion de idempotencia, persistencia, auditoria
 * y outbox, todo dentro de la misma transaccion.</p>
 */
@Service
public class CreateReservationWriter { // frontera transaccional de la creacion

    static final String IDEMPOTENCY_ENDPOINT = "CREATE_RESERVATION"; // identificador del endpoint para idempotencia

    private final IdempotencyPort idempotencyPort; // reclamacion y cierre de la clave de idempotencia

    private final InitiateReservationUseCase initiateReservation; // punto de entrada de creacion de CU-08

    private final ReservationRepositoryPort reservationRepository; // lectura de la reserva ya creada

    public CreateReservationWriter(IdempotencyPort idempotencyPort,
                                   InitiateReservationUseCase initiateReservation,
                                   ReservationRepositoryPort reservationRepository) {
        this.idempotencyPort = idempotencyPort; // asigna idempotencia
        this.initiateReservation = initiateReservation; // asigna motor CU-08
        this.reservationRepository = reservationRepository; // asigna repositorio
    }

    @Transactional
    public Reservation createOrReturnExisting(String normalizedKey,
                                              Instant now,
                                              Duration window,
                                              InitiateReservationCommand command) { // una sola transaccion atomica
        if (!idempotencyPort.tryClaim(normalizedKey, IDEMPOTENCY_ENDPOINT, now, window)) { // clave ya reclamada en la ventana
            UUID reservationId = idempotencyPort.findReservationId(normalizedKey, IDEMPOTENCY_ENDPOINT) // reserva original
                    .orElseThrow(() -> new ReservationRuleException(ErrorCode.INTERNAL_ERROR,
                            "Existing idempotent reservation id not found")); // falla si no hay id
            return reservationRepository.findById(reservationId) // recupera la reserva original
                    .orElseThrow(() -> new ReservationRuleException(ErrorCode.INTERNAL_ERROR,
                            "Existing idempotent reservation not found")); // no crea una segunda reserva
        }

        Reservation created = initiateReservation.initiate(command); // persiste, audita y encola en esta transaccion
        idempotencyPort.complete(normalizedKey, IDEMPOTENCY_ENDPOINT, created.getId()); // asocia la clave a la reserva
        return created; // devuelve la reserva creada
    }
}
