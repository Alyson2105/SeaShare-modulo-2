package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ReportNoShowCommand;
import com.seashare.m2.reservas.application.dto.ReportNoShowResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.ReportNoShowUseCase;
import com.seashare.m2.reservas.application.port.out.NoShowEventPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
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

@Service
@Transactional
public class ReportNoShowService implements ReportNoShowUseCase { // CU-05: reporte de inasistencia

    private static final int MAX_OBSERVATIONS_LENGTH = 500; // limite del texto libre de observaciones

    private static final Duration NO_SHOW_TOLERANCE = Duration.ofMinutes(30); // tolerancia fija de inasistencia

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // formato del zarpe pactado

    private static final String MESSAGE_WITH_AVAILABLE = "Inasistencia confirmada. La reserva ha sido cancelada y la embarcacion quedo Disponible. La compensacion al propietario sera gestionada por el sistema de pagos."; // aviso a M1 con liberacion

    private static final String MESSAGE_WITHOUT_AVAILABLE = "Inasistencia confirmada. La reserva ha sido cancelada. La compensacion al propietario sera gestionada por el sistema de pagos."; // sin aviso a M1

    private final ReservationRepositoryPort repository; // lectura de la reserva

    private final ChangeReservationStatusUseCase changeReservationStatusUseCase; // unico escritor de estado

    private final NoShowEventPort noShowEventPort; // persistencia del evento de inasistencia

    private final ClockPort clockPort; // reloj inyectable

    private final ModuleNotificationMatrix notificationMatrix; // matriz de avisos a M1 y M3

    public ReportNoShowService(ReservationRepositoryPort repository,
                               ChangeReservationStatusUseCase changeReservationStatusUseCase,
                               NoShowEventPort noShowEventPort,
                               ClockPort clockPort,
                               ModuleNotificationMatrix notificationMatrix) { // constructor con puertos y matriz
        this.repository = repository; // asigna repositorio
        this.changeReservationStatusUseCase = changeReservationStatusUseCase; // asigna motor de estado
        this.noShowEventPort = noShowEventPort; // asigna persistencia del evento
        this.clockPort = clockPort; // asigna reloj
        this.notificationMatrix = notificationMatrix; // asigna matriz de avisos
    }

    @Override
    public ReportNoShowResult execute(ReportNoShowCommand command) { // ejecuta el reporte completo en una transaccion
        Reservation reservation = repository.findById(command.reservationId()) // busca la reserva
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId())); // 404 si no existe

        validateIdentity(reservation, command); // solo el propietario puede reportar
        String observations = normalizeObservations(command.observations()); // normaliza y valida las observaciones
        validateState(reservation.getStatus()); // exige RESERVED antes de cualquier escritura
        ZoneId zone = resolveZone(reservation.getHarborTimezone()); // zona guardada en la reserva

        Instant now = clockPort.now(); // instante actual del sistema, una sola vez
        OffsetDateTime scheduledDeparture = HarborTime.at( // zarpe pactado en la zona del puerto
                reservation.getStartDate(), reservation.getDepartureTime(), zone); // fecha, hora y zona
        Instant scheduled = scheduledDeparture.toInstant(); // zarpe pactado como instante

        if (now.isBefore(scheduled)) { // aun no llega la hora pactada
            throw new ReservationRuleException(ErrorCode.TRIP_NOT_STARTED,
                    "No-show cannot be reported before the scheduled departure at " + scheduledDeparture.format(ISO_OFFSET)); // 409
        }

        Instant enabledAt = scheduled.plus(NO_SHOW_TOLERANCE); // inicio del reporte habilitado
        if (now.isBefore(enabledAt)) { // tolerancia de 30 minutos aun vigente
            Duration remaining = Duration.between(now, enabledAt); // tiempo restante
            long totalSeconds = remaining.getSeconds(); // segundos completos restantes
            if (remaining.getNano() > 0) { // ajusta fracciones de segundo hacia arriba
                totalSeconds++; // evita reportar cero segundos cuando aun falta fraccion
            }
            long minutes = totalSeconds / 60; // minutos restantes
            long seconds = totalSeconds % 60; // segundos restantes
            throw new ReservationRuleException(ErrorCode.GRACE_PERIOD_NOT_ELAPSED,
                    "Grace period still active. " + minutes + " minutes and " + seconds
                            + " seconds remaining before no-show can be reported"); // 409
        }

        long waitMinutes = Duration.between(scheduled, now).toMinutes(); // minutos completos esperados

        OperationalStatus vesselOperationalStatus = notificationMatrix.decide( // calcula el aviso a M1
                ReservationStatus.RESERVED,
                ReservationStatus.CANCELLED,
                false,
                reservation.isM1Blocked()).moduleOneStatus(); // estado operativo o null

        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand( // delega el cambio de estado
                command.reservationId(), // reserva
                ReservationStatus.CANCELLED, // estado destino
                CancellationSubtype.NO_SHOW, // subtipo obligatorio
                ActorType.OWNER, // actor propietario
                null, // sin motivo obligatorio en CU-08
                command.callerId(), // actor_id
                null, // expectedVersion: el servicio no envia version
                false, // sin averia mecanica
                null, // sin anticipacion
                null, // completionNotes: solo se acepta en COMPLETED
                null, // sin referencia de pago
                null)); // sin token de pago

        noShowEventPort.save( // inserta el evento inmutable solo despues de CU-08
                command.reservationId(),
                command.callerId(),
                now,
                (int) waitMinutes,
                observations);

        return new ReportNoShowResult(
                command.reservationId(), // reserva
                ReservationStatus.CANCELLED, // estado resultante
                CancellationSubtype.NO_SHOW, // subtipo resultante
                now.atZone(zone).toOffsetDateTime(), // cancelacion con offset del puerto
                waitMinutes, // minutos completos esperados
                reservation.getVesselId(), // embarcacion
                vesselOperationalStatus, // aviso a M1 o null
                vesselOperationalStatus != null ? MESSAGE_WITH_AVAILABLE : MESSAGE_WITHOUT_AVAILABLE); // mensaje sin tildes
    }

    private void validateIdentity(Reservation reservation, ReportNoShowCommand command) { // valida rol y pertenencia
        if (command.actor() != ActorType.OWNER || !reservation.isOwner(command.callerId())) { // solo el propietario titular
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only the reservation owner is authorized to report a no-show"); // 403
        }
    }

    private String normalizeObservations(String observations) { // valida y normaliza las observaciones
        if (observations == null || observations.isBlank()) { // ausente o solo espacios
            return null; // se trata como sin observaciones
        }
        if (observations.length() > MAX_OBSERVATIONS_LENGTH) { // supera el limite
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "observations must not exceed 500 characters"); // 400
        }
        return observations; // conserva el texto original
    }

    private void validateState(ReservationStatus status) { // valida el estado antes de escribir
        if (status != ReservationStatus.RESERVED) { // estado distinto del esperado
            throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "No-show can only be reported from status RESERVED, current status is " + status); // 409
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
}