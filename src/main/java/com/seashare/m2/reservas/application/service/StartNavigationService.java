package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.StartNavigationCommand;
import com.seashare.m2.reservas.application.dto.StartNavigationResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.StartNavigationUseCase;
import com.seashare.m2.reservas.application.port.out.CheckinEventPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class StartNavigationService implements StartNavigationUseCase { // CU-06: inicio de navegacion y check-in

    private static final int MAX_DELIVERY_NOTES_LENGTH = 1000; // limite del texto libre de entrega

    private static final Duration NO_SHOW_TOLERANCE = Duration.ofMinutes(30); // tolerancia fija de inasistencia

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // formato del zarpe pactado

    private static final String MESSAGE_PUNCTUAL = "Inicio de navegacion registrado exitosamente. La reserva ha pasado a En Navegacion y se encolo la notificacion a Modulo 3 para activar el seguro nautico."; // inicio puntual con evento a M3

    private static final String MESSAGE_PUNCTUAL_WITHOUT_INSURANCE = "Inicio de navegacion registrado exitosamente. La reserva ha pasado a En Navegacion."; // inicio puntual sin evento a M3

    private static final String MESSAGE_LATE = "Inicio de navegacion registrado exitosamente tras llegada tardia. La opcion de inasistencia ha quedado inhabilitada de forma permanente."; // inicio tardio

    private final ReservationRepositoryPort repository; // lectura y persistencia de la reserva

    private final ChangeReservationStatusUseCase changeReservationStatusUseCase; // unico escritor de estado

    private final CheckinEventPort checkinEventPort; // persistencia del evento de check-in

    private final ClockPort clockPort; // reloj inyectable

    private final ModuleNotificationMatrix notificationMatrix; // matriz de avisos a M1 y M3

    public StartNavigationService(ReservationRepositoryPort repository,
                                  ChangeReservationStatusUseCase changeReservationStatusUseCase,
                                  CheckinEventPort checkinEventPort,
                                  ClockPort clockPort,
                                  ModuleNotificationMatrix notificationMatrix) { // constructor con puertos y matriz
        this.repository = repository; // asigna repositorio
        this.changeReservationStatusUseCase = changeReservationStatusUseCase; // asigna motor de estado
        this.checkinEventPort = checkinEventPort; // asigna persistencia del evento
        this.clockPort = clockPort; // asigna reloj
        this.notificationMatrix = notificationMatrix; // asigna matriz de avisos
    }

    @Override
    public StartNavigationResult execute(StartNavigationCommand command) { // ejecuta el inicio completo en una transaccion
        Reservation reservation = repository.findById(command.reservationId()) // busca la reserva
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId())); // 404 si no existe

        validateIdentity(reservation, command); // solo el propietario puede iniciar la navegacion
        String deliveryNotes = normalizeDeliveryNotes(command.deliveryNotes()); // normaliza y valida las notas
        validateState(reservation.getStatus()); // valida el estado antes de escribir
        ZoneId zone = resolveZone(reservation.getHarborTimezone()); // zona guardada en la reserva, sin llamar a M1

        Instant now = clockPort.now(); // instante actual del sistema, una sola vez
        OffsetDateTime scheduledDeparture = HarborTime.at( // zarpe pactado en la zona del puerto
                reservation.getStartDate(), reservation.getDepartureTime(), zone); // fecha, hora y zona
        Instant scheduled = scheduledDeparture.toInstant(); // zarpe pactado como instante

        if (now.isBefore(scheduled)) { // el inicio no se permite antes de la hora pactada
            throw new ReservationRuleException(ErrorCode.EARLY_DEPARTURE,
                    "Navigation cannot start before the scheduled departure at " + scheduledDeparture.format(ISO_OFFSET)); // 409
        }

        boolean informedDeparture = command.actualDepartureAt() != null; // indica si se informo la salida
        Instant departure = informedDeparture ? command.actualDepartureAt().toInstant() : now; // salida real
        if (informedDeparture && departure.isBefore(scheduled)) { // salida informada anterior a la pactada
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "actual_departure_at cannot be before the scheduled departure"); // 400
        }

        if (repository.existsOtherInNavigation(reservation.getVesselId(), reservation.getId())) { // otra travesia activa
            throw new ReservationRuleException(ErrorCode.VESSEL_CURRENTLY_IN_NAVIGATION,
                    "The vessel is currently in navigation for another reservation"); // 409
        }

        reservation.setActualDepartureAt(departure); // fija la salida real en el agregado
        repository.save(reservation); // persiste la salida antes de delegar el estado a CU-08

        OperationalStatus vesselOperationalStatus = notificationMatrix.decide( // calcula el aviso a M1
                ReservationStatus.RESERVED,
                ReservationStatus.IN_NAVIGATION,
                false,
                reservation.isM1Blocked()).moduleOneStatus(); // estado operativo o null

        ChangeReservationStatusResult statusResult = changeReservationStatusUseCase.execute( // delega el cambio de estado
                new ChangeReservationStatusCommand(
                        command.reservationId(), // reserva
                        ReservationStatus.IN_NAVIGATION, // estado destino
                        null, // sin subtipo de cancelacion
                        ActorType.OWNER, // actor propietario
                        null, // sin motivo de auditoria
                        command.callerId(), // actor_id
                        null, // expectedVersion: la version ya subio en el paso anterior
                        false, // sin averia mecanica
                        null, // sin anticipacion
                        null, // completionNotes: solo se acepta en COMPLETED
                        null, // sin referencia de pago
                        null)); // sin token de pago

        checkinEventPort.save( // inserta el evento inmutable solo despues de CU-08
                command.reservationId(),
                command.callerId(),
                departure,
                deliveryNotes);

        boolean insuranceActivated = containsDestination( // la poliza se activa si M3 fue encolado
                statusResult.eventsEnqueued(), ChangeReservationStatusService.DESTINATION_AMQP); // destino AMQP
        boolean late = departure.isAfter(scheduled.plus(NO_SHOW_TOLERANCE)); // tardanza estrictamente mayor a 30 minutos

        return new StartNavigationResult(
                command.reservationId(), // reserva
                statusResult.status(), // estado resultante
                departure.atZone(zone).toOffsetDateTime(), // salida con offset del puerto
                reservation.getVesselId(), // embarcacion
                vesselOperationalStatus, // aviso a M1 o null
                insuranceActivated, // resultado del evento a M3
                messageFor(late, insuranceActivated)); // mensaje sin tildes
    }

    private void validateIdentity(Reservation reservation, StartNavigationCommand command) { // valida rol y pertenencia
        if (command.actor() != ActorType.OWNER || !reservation.isOwner(command.callerId())) { // solo el propietario titular
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only the reservation owner is authorized to start navigation"); // 403
        }
    }

    private String normalizeDeliveryNotes(String deliveryNotes) { // valida y normaliza las notas
        if (deliveryNotes == null || deliveryNotes.isBlank()) { // ausente o solo espacios
            return null; // se trata como sin notas
        }
        if (deliveryNotes.length() > MAX_DELIVERY_NOTES_LENGTH) { // supera el limite
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "delivery_notes must not exceed 1000 characters"); // 400
        }
        return deliveryNotes; // conserva el texto original
    }

    private void validateState(ReservationStatus status) { // valida el estado antes de escribir
        if (status == ReservationStatus.IN_NAVIGATION) { // inicio repetido
            throw new ReservationRuleException(ErrorCode.TRIP_ALREADY_STARTED,
                    "Navigation has already been started for this reservation"); // 409
        }
        if (status != ReservationStatus.RESERVED) { // estado distinto del esperado
            throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "Navigation cannot start from status " + status); // 409
        }
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
        return eventsEnqueued != null && eventsEnqueued.contains(destination); // evita npe si no hay lista
    }

    private String messageFor(boolean late, boolean insuranceActivated) { // selecciona el mensaje informativo
        if (late) { // llegada tardia
            return MESSAGE_LATE; // inhabilitacion de inasistencia
        }
        return insuranceActivated ? MESSAGE_PUNCTUAL : MESSAGE_PUNCTUAL_WITHOUT_INSURANCE; // inicio puntual
    }
}