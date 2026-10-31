package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.StatusAuditEntry;
import com.seashare.m2.reservas.application.port.out.StatusAuditPort;
import com.seashare.m2.reservas.application.port.out.StatusChangedEvent;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.state.InvalidTransitionException;
import com.seashare.m2.reservas.domain.state.ReservationStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ChangeReservationStatusService implements ChangeReservationStatusUseCase { // servicio transaccional para cambio de estado

    private static final Set<ReservationStatus> OUTBOX_STATUSES = EnumSet.of(
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.IN_NAVIGATION,
            ReservationStatus.COMPLETED,
            ReservationStatus.CANCELLED
    ); // estados destino que generan evento outbox

    private final ReservationRepositoryPort repository; // puerto de persistencia de reservas
    private final StatusAuditPort statusAuditPort; // puerto de auditoria de estados
    private final OutboxPort outboxPort; // puerto de mensajeria outbox
    private final ReservationStateMachine machine = new ReservationStateMachine(); // maquina de estados de reservas

    public ChangeReservationStatusService(ReservationRepositoryPort repository,
                                          StatusAuditPort statusAuditPort,
                                          OutboxPort outboxPort) { // constructor con inyeccion de puertos
        this.repository = repository; // asigna repositorio
        this.statusAuditPort = statusAuditPort; // asigna auditoria
        this.outboxPort = outboxPort; // asigna outbox
    }

    @Override
    public void execute(ChangeReservationStatusCommand command) { // ejecuta el flujo transaccional de cambio de estado
        Instant changedAt = Instant.now(); // reloj temporal a reemplazar en proximo issue

        Reservation reservation = repository.findById(command.reservationId()) // busca reserva por id
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId())); // lanza excepcion si no existe

        validateActorForSubtype(reservation.getStatus(), command.targetStatus(), command.subtype(), command.actor()); // valida actor segun subtipo

        ReservationStatus previousStatus = reservation.getStatus(); // guarda estado anterior
        reservation.changeStatus(command.targetStatus(), command.subtype(), machine); // aplica transicion de estado en el dominio

        repository.save(reservation); // guarda cambios en la reserva

        statusAuditPort.record(new StatusAuditEntry( // registra auditoria inmutable
                reservation.getId(), // id de la reserva
                previousStatus, // estado de origen
                reservation.getStatus(), // estado de destino
                reservation.getCancellationSubtype(), // subtipo de cancelacion efectivo
                command.actor(), // actor de la operacion
                command.reason(), // motivo o justificacion
                changedAt // marca temporal del cambio
        ));

        if (OUTBOX_STATUSES.contains(reservation.getStatus())) { // evalua si el destino requiere encolado
            outboxPort.enqueue(new StatusChangedEvent( // encola evento outbox
                    UUID.randomUUID(), // id unico del evento
                    reservation.getId(), // id de la reserva
                    reservation.getStatus(), // nuevo estado
                    reservation.getCancellationSubtype(), // subtipo de cancelacion efectivo
                    changedAt // marca temporal del cambio
            ));
        }
    }

    private void validateActorForSubtype(ReservationStatus from,
                                        ReservationStatus to,
                                        CancellationSubtype subtype,
                                        ActorType actor) { // valida correspondencia entre actor y subtipo
        if (subtype == null) { // subtipo nulo permite cualquier actor
            return; // transicion valida sin subtipo
        }
        if (actor == ActorType.SYSTEM) { // actor de sistema con subtipo no nulo
            throw new InvalidTransitionException(from, to, "SYSTEM actor is not allowed with cancellation subtype"); // rechaza actor de sistema
        }
        boolean allowed = switch (subtype) { // evalua restricciones por subtipo
            case FLEXIBLE, MODERATE, LATE -> actor == ActorType.RENTER; // solo arrendatario
            case BY_OWNER, NO_SHOW -> actor == ActorType.OWNER; // solo propietario
        };
        if (!allowed) { // actor no autorizado para el subtipo
            throw new InvalidTransitionException(from, to, "Actor " + actor + " is not allowed for subtype " + subtype); // rechaza combinacion invalida
        }
    }
}
