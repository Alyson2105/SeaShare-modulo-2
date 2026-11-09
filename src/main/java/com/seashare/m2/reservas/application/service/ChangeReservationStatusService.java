package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.StatusAuditEntry;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.StatusAuditPort;
import com.seashare.m2.reservas.domain.exception.InvalidTransitionException;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.state.ReservationStateMachine;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ChangeReservationStatusService implements ChangeReservationStatusUseCase { // servicio transaccional para cambio de estado

    static final String DESTINATION_AMQP = "AMQP"; // destino outbox hacia modulo 3 (CU-14)

    private static final Set<ReservationStatus> OUTBOX_STATUSES = EnumSet.of( // estados destino que generan evento outbox
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.IN_NAVIGATION,
            ReservationStatus.COMPLETED,
            ReservationStatus.CANCELLED
    ); // estados destino que generan evento outbox

    private final ReservationRepositoryPort repository; // puerto de persistencia de reservas
    private final StatusAuditPort statusAuditPort; // puerto de auditoria de estados
    private final OutboxPort outboxPort; // puerto de mensajeria outbox
    private final ClockPort clockPort; // puerto de reloj para marca temporal
    private final ReservationStateMachine machine = new ReservationStateMachine(); // maquina de estados de reservas

    public ChangeReservationStatusService(ReservationRepositoryPort repository,
                                          StatusAuditPort statusAuditPort,
                                          OutboxPort outboxPort,
                                          ClockPort clockPort) { // constructor con inyeccion de puertos
        this.repository = repository; // asigna repositorio
        this.statusAuditPort = statusAuditPort; // asigna auditoria
        this.outboxPort = outboxPort; // asigna outbox
        this.clockPort = clockPort; // asigna reloj
    }

    @Override
    public ChangeReservationStatusResult execute(ChangeReservationStatusCommand command) { // ejecuta el flujo transaccional de cambio de estado
        Instant changedAt = clockPort.now(); // captura instante actual provisto por el puerto de reloj

        Reservation reservation = repository.findById(command.reservationId()) // busca reserva por id
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId())); // lanza excepcion si no existe

        ReservationStatus previousStatus = reservation.getStatus(); // guarda estado anterior
        validateCommand(reservation, previousStatus, command, changedAt); // valida actor, subtipo, campos condicionales, version y ttl

        reservation.changeStatus(command.targetStatus(), command.subtype(), machine); // aplica transicion de estado en el dominio
        applyCommandFields(reservation, command, changedAt); // fija campos de CU-08 persistidos (T-09)
        Reservation saved = repository.save(reservation); // guarda cambios y retorna la version actualizada

        List<String> eventsEnqueued = new ArrayList<>(); // destinos con evento encolado
        List<UUID> eventIds = new ArrayList<>(); // ids de los eventos de esta transaccion (FR-011)
        boolean enqueueM3 = OUTBOX_STATUSES.contains(saved.getStatus()); // el destino requiere evento a modulo 3
        UUID m3EventId = UUID.randomUUID(); // id generado en la misma transaccion antes de auditar
        if (enqueueM3) { // solo si corresponde
            eventIds.add(m3EventId); // registra el id para la auditoria
            eventsEnqueued.add(DESTINATION_AMQP); // registra destino AMQP (modulo 3)
        }

        statusAuditPort.record(new StatusAuditEntry( // registra auditoria inmutable
                saved.getId(), // id de la reserva
                previousStatus, // estado de origen
                saved.getStatus(), // estado de destino
                saved.getCancellationSubtype(), // subtipo de cancelacion efectivo
                command.actor(), // actor de la operacion
                command.reason(), // motivo o justificacion
                changedAt, // marca temporal del cambio
                command.actorId(), // id del usuario; nulo si actor es SYSTEM
                List.copyOf(eventIds) // eventos encolados en esta transaccion
        ));

        if (enqueueM3) { // encola el evento con el id ya registrado en auditoria
            outboxPort.enqueue(new StatusChangedEvent( // encola evento outbox
                    m3EventId, // id unico generado arriba (mismo para auditoria y outbox)
                    saved.getId(), // id de la reserva
                    saved.getStatus(), // nuevo estado
                    saved.getCancellationSubtype(), // subtipo de cancelacion efectivo
                    changedAt, // marca temporal del cambio
                    command.actor(), // actor que disparo la transicion (CU-14 FR-002)
                    command.anticipationMinutes(), // anticipacion al cancelar (CU-14 FR-003)
                    command.completionNotes() // notas de cierre en COMPLETED (CU-14 FR-004)
            ));
        }

        return new ChangeReservationStatusResult( // construye el resultado tipado del contrato
                saved.getId(), // id de la reserva
                saved.getStatus(), // estado resultante
                saved.getCancellationSubtype(), // subtipo resultante o null
                changedAt, // hora exacta del cambio
                saved.getTtlExpiresAt(), // expiracion del ttl sin cambios
                saved.getVersion(), // version incrementada tras el guardado
                List.copyOf(eventsEnqueued) // lista inmutable de destinos encolados
        );
    }

    private void validateCommand(Reservation reservation,
                                 ReservationStatus from,
                                 ChangeReservationStatusCommand command,
                                 Instant changedAt) { // valida el comando completo contra el contrato de CU-08
        validateCancellationOrigin(from, command.targetStatus()); // cancelacion activa solo desde RESERVED (spec L156-L159)
        if (!machine.canTransition(from, command.targetStatus())) { // transicion fuera de la matriz o estado terminal
            throw new InvalidTransitionException(from, command.targetStatus(),
                    "Cannot transition from " + from + " to " + command.targetStatus()); // rechaza transicion no permitida
        }
        validateSubtypeTargetCombination(from, command.targetStatus(), command.subtype()); // subtipo obligatorio con CANCELLED y prohibido fuera
        validateActorForSubtype(from, command.targetStatus(), command.subtype(), command.actor()); // valida actor segun subtipo
        validateActorIdentity(command); // actor_id obligatorio si actor no es SYSTEM
        validateFieldPerTarget(command); // completion_notes solo COMPLETED; payment_reference solo RESERVED
        validateCancellationData(command); // anticipacion al cancelar RENTER; averia al cancelar OWNER
        validateExpectedVersion(reservation, command); // expected_version opcional vs version actual
        validateTtlForReserved(reservation, command.targetStatus(), changedAt); // RESERVED exige ttl vigente (corte exacto)
    }

    private void validateCancellationOrigin(ReservationStatus from, ReservationStatus to) { // cancelacion activa solo desde RESERVED
        if (to == ReservationStatus.CANCELLED && from != ReservationStatus.RESERVED) { // cancelacion fuera de RESERVED (spec L156-L159)
            throw new InvalidTransitionException(from, to,
                    "Solo las reservas en estado RESERVED admiten cancelacion activa; desde " + from
                            + " la reserva debe liberarse pasivamente por el TTL"); // explica el rechazo
        }
    }

    private void applyCommandFields(Reservation reservation, ChangeReservationStatusCommand command, Instant changedAt) { // persiste campos de CU-08
        reservation.setStatusChangedAt(changedAt); // hora exacta del cambio en toda transicion
        if (command.targetStatus() == ReservationStatus.RESERVED) { // confirmacion de pago
            reservation.setPaymentConfirmedAt(changedAt); // guarda la hora de confirmacion
            if (command.paymentReference() != null) { // referencia opcional
                reservation.setPaymentReference(command.paymentReference()); // persiste la referencia del cobro
            }
        }
        if (command.targetStatus() == ReservationStatus.COMPLETED && command.completionNotes() != null) { // cierre con novedades
            reservation.setCompletionNotes(command.completionNotes()); // persiste el texto de novedades (FR-009)
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

    private void validateSubtypeTargetCombination(ReservationStatus from,
                                                  ReservationStatus to,
                                                  CancellationSubtype subtype) { // subtipo obligatorio con CANCELLED y prohibido con los demas estados
        if (to == ReservationStatus.CANCELLED && subtype == null) { // cancelacion sin subtipo
            throw new InvalidTransitionException(from, to, "Cancellation subtype is required when transitioning to CANCELLED"); // exige subtipo
        }
        if (to != ReservationStatus.CANCELLED && subtype != null) { // subtipo con estado que no es cancelacion
            throw new InvalidTransitionException(from, to, "Cancellation subtype is only allowed when transitioning to CANCELLED"); // rechaza subtipo indebido
        }
    }

    private void validateActorIdentity(ChangeReservationStatusCommand command) { // actor_id obligatorio si actor no es SYSTEM
        if (command.actor() != ActorType.SYSTEM && command.actorId() == null) { // actor humano sin identidad
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "actor_id is required when actor is not SYSTEM"); // rechaza comando incompleto
        }
    }

    private void validateFieldPerTarget(ChangeReservationStatusCommand command) { // campos opcionales restringidos a un destino
        if (command.completionNotes() != null && command.targetStatus() != ReservationStatus.COMPLETED) { // notas fuera de COMPLETED
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "completion_notes is only allowed when transitioning to COMPLETED"); // rechaza
        }
        if (command.paymentReference() != null && command.targetStatus() != ReservationStatus.RESERVED) { // referencia fuera de RESERVED
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "payment_reference is only allowed when transitioning to RESERVED"); // rechaza
        }
    }

    private void validateCancellationData(ChangeReservationStatusCommand command) { // campos obligatorios al cancelar segun actor
        if (command.targetStatus() != ReservationStatus.CANCELLED) { // fuera de cancelacion no aplica
            return; // sin validacion
        }
        if (command.actor() == ActorType.RENTER && command.anticipationMinutes() == null) { // arrendatario cancela sin anticipacion
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "anticipation_minutes is required when a renter cancels"); // exige anticipacion
        }
        if (command.actor() == ActorType.OWNER && command.mechanicalFailure() == null) { // propietario cancela sin indicador de averia
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "mechanical_failure is required when an owner cancels"); // exige averia
        }
    }

    private void validateExpectedVersion(Reservation reservation, ChangeReservationStatusCommand command) { // expected_version opcional
        if (command.expectedVersion() != null && !Objects.equals(command.expectedVersion(), reservation.getVersion())) { // version distinta a la vista
            throw new VersionConflictException("Expected version " + command.expectedVersion()
                    + " does not match current version " + reservation.getVersion() + " for reservation " + reservation.getId()); // conflicto de concurrencia
        }
    }

    private void validateTtlForReserved(Reservation reservation, ReservationStatus target, Instant changedAt) { // RESERVED exige ttl vigente
        if (target != ReservationStatus.RESERVED) { // la guarda solo aplica a RESERVED
            return; // sin validacion
        }
        Instant ttlExpiresAt = reservation.getTtlExpiresAt(); // expiracion de la reserva
        if (ttlExpiresAt != null && !changedAt.isBefore(ttlExpiresAt)) { // corte exacto: en el segundo del vencimiento ya expiro
            throw new ReservationRuleException(ErrorCode.RESERVATION_EXPIRED,
                    "The 15 minute time limit of the reservation has expired"); // rechaza RESERVED con ttl vencido
        }
    }
}
