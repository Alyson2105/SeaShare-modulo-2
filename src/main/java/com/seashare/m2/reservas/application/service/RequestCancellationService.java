package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CancellationEventRecord;
import com.seashare.m2.reservas.application.dto.CancellationResult;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.RequestCancellationCommand;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.RequestCancellationUseCase;
import com.seashare.m2.reservas.application.port.out.CancellationEventPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.service.CancellationClassifier;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class RequestCancellationService implements RequestCancellationUseCase { // CU-04: cancelacion de una reserva RESERVED

    private static final String REASON_LOGISTICS_FORCE_MAJEURE = "logistics_force_majeure"; // motivo de fuerza mayor

    private static final String REASON_MECHANICAL_FAILURE = "mechanical_failure"; // motivo de averia mecanica

    private static final int MAX_JUSTIFICATION_LENGTH = 500; // limite del texto libre

    private static final String M1_SYNC_ENQUEUED = "ENQUEUED"; // aviso a M1 encolado

    private static final String M3_NOTIFICATION_PUBLISHED = "PUBLISHED"; // evento a M3 encolado

    private final ReservationRepositoryPort repository; // lectura de la reserva

    private final ChangeReservationStatusUseCase changeReservationStatusUseCase; // unico escritor de estado

    private final CancellationEventPort cancellationEventPort; // persistencia del evento de cancelacion

    private final ClockPort clockPort; // reloj inyectable

    private final ModuleNotificationMatrix notificationMatrix; // matriz de avisos a M1 y M3

    private final CancellationClassifier classifier = new CancellationClassifier(); // clasificador puro de dominio

    public RequestCancellationService(ReservationRepositoryPort repository,
                                      ChangeReservationStatusUseCase changeReservationStatusUseCase,
                                      CancellationEventPort cancellationEventPort,
                                      ClockPort clockPort,
                                      ModuleNotificationMatrix notificationMatrix) { // constructor con puertos y matriz
        this.repository = repository; // asigna repositorio
        this.changeReservationStatusUseCase = changeReservationStatusUseCase; // asigna motor de estado
        this.cancellationEventPort = cancellationEventPort; // asigna persistencia del evento
        this.clockPort = clockPort; // asigna reloj
        this.notificationMatrix = notificationMatrix; // asigna matriz de avisos
    }

    @Override
    public CancellationResult execute(RequestCancellationCommand command) { // ejecuta el flujo completo de CU-04
        Reservation reservation = repository.findById(command.reservationId()) // busca la reserva
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId())); // 404 si no existe

        validateIdentity(reservation, command); // valida que el actor sea el titular o el propietario
        String ownerReason = validateOwnerReason(command); // valida y normaliza el motivo del propietario
        String justification = normalizeJustification(command.justification()); // normaliza la justificacion

        if (reservation.getStatus() != ReservationStatus.RESERVED) { // solo se cancela desde RESERVED
            throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "Reservation cannot be cancelled from status " + reservation.getStatus()); // estado invalido
        }

        ZoneId zone = resolveZone(reservation.getHarborTimezone()); // zona guardada en la reserva, sin llamar a M1
        if (reservation.getStartDate() == null || reservation.getDepartureTime() == null) { // datos minimos del zarpe
            throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR,
                    "Reservation is missing start date or departure time"); // dato inconsistente
        }
        OffsetDateTime scheduledDeparture = HarborTime.at(reservation.getStartDate(), reservation.getDepartureTime(), zone); // zarpe programado

        Instant now = clockPort.now(); // instante unico de la solicitud

        CancellationSubtype subtype; // clasificacion final
        Integer anticipationMinutes; // minutos para CU-08; null para OWNER
        boolean mechanicalFailure; // averia reportada por el propietario

        if (command.actor() == ActorType.RENTER) { // el arrendatario se clasifica por tiempo
            CancellationClassifier.Classification classification =
                    classifier.classifyForRenter(now, scheduledDeparture.toInstant()); // clasifica la anticipacion
            subtype = classification.subtype(); // subtipo calculado
            anticipationMinutes = classification.anticipationMinutes(); // minutos truncados
            mechanicalFailure = false; // el arrendatario nunca reporta averia
        } else { // el propietario cancela sin medir tiempo
            subtype = CancellationSubtype.BY_OWNER; // subtipo fijo
            anticipationMinutes = null; // sin anticipacion
            mechanicalFailure = REASON_MECHANICAL_FAILURE.equals(ownerReason); // averia solo si el motivo lo indica
        }

        OperationalStatus vesselNewOperationalStatus = notificationMatrix.decide( // calcula el aviso a M1 antes de CU-08
                ReservationStatus.RESERVED,
                ReservationStatus.CANCELLED,
                mechanicalFailure,
                reservation.isM1Blocked()).moduleOneStatus(); // estado operativo o null

        String auditReason = command.actor() == ActorType.OWNER ? ownerReason : justification; // motivo de auditoria

        ChangeReservationStatusResult statusResult = changeReservationStatusUseCase.execute( // delega el cambio de estado
                new ChangeReservationStatusCommand(
                        command.reservationId(), // reserva
                        ReservationStatus.CANCELLED, // estado destino
                        subtype, // subtipo de cancelacion
                        command.actor(), // actor
                        auditReason, // motivo de auditoria
                        command.callerId(), // actor_id
                        null, // expectedVersion: se delega al @Version de CU-08
                        mechanicalFailure, // averia reportada
                        anticipationMinutes, // minutos solo para RENTER
                        null, // completionNotes
                        null, // paymentReference
                        null)); // paymentTokenRef

        long eventMinutes = anticipationMinutes == null ? 0L : anticipationMinutes.longValue(); // OWNER registra cero
        cancellationEventPort.save(new CancellationEventRecord( // evento inmutable solo despues de CU-08
                command.reservationId(),
                command.actor(),
                now,
                eventMinutes,
                subtype,
                ownerReason,
                justification));

        Double anticipationHours = anticipationMinutes == null ? null : anticipationMinutes / 60.0; // horas de anticipacion
        String m1SyncStatus = containsDestination(statusResult.eventsEnqueued(),
                ChangeReservationStatusService.DESTINATION_M1_HTTP) ? M1_SYNC_ENQUEUED : null; // aviso M1
        String m3NotificationStatus = containsDestination(statusResult.eventsEnqueued(),
                ChangeReservationStatusService.DESTINATION_AMQP) ? M3_NOTIFICATION_PUBLISHED : null; // evento M3

        return new CancellationResult(
                command.reservationId(), // reserva
                statusResult.status(), // estado resultante
                subtype, // subtipo aplicado
                command.actor(), // actor solicitante
                anticipationHours, // horas o null
                zone.getId(), // zona horaria del puerto
                scheduledDeparture, // zarpe con offset
                now.atZone(zone).toOffsetDateTime(), // instante de la solicitud con offset
                ownerReason, // motivo del propietario o null
                justification, // justificacion normalizada
                reservation.getVesselId(), // embarcacion
                vesselNewOperationalStatus, // aviso a M1 o null
                m1SyncStatus, // estado del aviso M1
                m3NotificationStatus); // estado del evento M3
    }

    private void validateIdentity(Reservation reservation, RequestCancellationCommand command) { // valida pertenencia segun rol
        boolean allowed = command.actor() == ActorType.RENTER
                ? reservation.isRenter(command.callerId())
                : command.actor() == ActorType.OWNER && reservation.isOwner(command.callerId()); // solo titular o propietario
        if (!allowed) { // rol desconocido o usuario ajeno
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED,
                    "The reservation does not belong to the caller for the requested role"); // 403
        }
    }

    private String validateOwnerReason(RequestCancellationCommand command) { // valida el motivo del propietario
        if (command.actor() != ActorType.OWNER) { // el arrendatario ignora el motivo
            return null; // sin motivo para RENTER
        }
        if (command.reason() == null) { // motivo obligatorio para OWNER
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "reason is required for OWNER cancellations"); // 400
        }
        String normalized = command.reason().trim(); // elimina espacios externos
        if (!REASON_LOGISTICS_FORCE_MAJEURE.equals(normalized) && !REASON_MECHANICAL_FAILURE.equals(normalized)) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "reason must be logistics_force_majeure or mechanical_failure"); // valor no soportado
        }
        return normalized; // motivo valido normalizado
    }

    private String normalizeJustification(String justification) { // valida y normaliza el texto libre
        if (justification == null || justification.isBlank()) { // ausente o vacio
            return null; // se persiste null
        }
        if (justification.length() > MAX_JUSTIFICATION_LENGTH) { // supera el limite
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "justification must not exceed 500 characters"); // 400
        }
        return justification; // texto original
    }

    private ZoneId resolveZone(String harborTimezone) { // resuelve la zona guardada sin fail-safe silencioso
        if (harborTimezone == null || harborTimezone.isBlank()) { // zona ausente
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE,
                    "Harbor timezone is missing in the reservation"); // 503
        }
        try {
            return ZoneId.of(harborTimezone.trim()); // zona IANA valida
        } catch (DateTimeException e) { // zona no interpretable
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE,
                    "Harbor timezone is invalid in the reservation"); // 503
        }
    }

    private boolean containsDestination(List<String> eventsEnqueued, String destination) { // consulta segura de destinos
        return eventsEnqueued != null && eventsEnqueued.contains(destination); // evita npe si el resultado no trae lista
    }
}