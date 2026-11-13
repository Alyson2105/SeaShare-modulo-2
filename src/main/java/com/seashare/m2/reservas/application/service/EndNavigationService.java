package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.EndNavigationCommand;
import com.seashare.m2.reservas.application.dto.EndNavigationResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.EndNavigationUseCase;
import com.seashare.m2.reservas.application.port.out.CheckoutEventPort;
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
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional
public class EndNavigationService implements EndNavigationUseCase { // CU-07: cierre de navegacion y evento de checkout

    private static final int MAX_REMARKS_LENGTH = 2000; // limite del texto libre de novedades

    private static final String MESSAGE_WITH_M1 = "Fin de navegacion registrado exitosamente. La reserva ha pasado a Completada y se encolo la senal para que Modulo 1 pase la embarcacion a En Mantenimiento/Limpieza."; // cierre con aviso a M1

    private static final String MESSAGE_WITH_M1_REMARKS = "Fin de navegacion registrado con novedades. La reserva ha pasado a Completada y se encolo la senal para que Modulo 1 pase la embarcacion a En Mantenimiento/Limpieza."; // cierre con novedades y aviso a M1

    private static final String MESSAGE_WITHOUT_M1 = "Fin de navegacion registrado exitosamente. La reserva ha pasado a Completada."; // cierre sin aviso a M1

    private static final String MESSAGE_WITHOUT_M1_REMARKS = "Fin de navegacion registrado con novedades. La reserva ha pasado a Completada."; // cierre con novedades sin aviso a M1

    private final ReservationRepositoryPort repository; // lectura y persistencia de la reserva

    private final ChangeReservationStatusUseCase changeReservationStatusUseCase; // unico escritor de estado

    private final CheckoutEventPort checkoutEventPort; // persistencia del evento de checkout

    private final ClockPort clockPort; // reloj inyectable

    private final ModuleNotificationMatrix notificationMatrix; // matriz de avisos a M1 y M3

    public EndNavigationService(ReservationRepositoryPort repository,
                                ChangeReservationStatusUseCase changeReservationStatusUseCase,
                                CheckoutEventPort checkoutEventPort,
                                ClockPort clockPort,
                                ModuleNotificationMatrix notificationMatrix) { // constructor con puertos y matriz
        this.repository = repository; // asigna repositorio
        this.changeReservationStatusUseCase = changeReservationStatusUseCase; // asigna motor de estado
        this.checkoutEventPort = checkoutEventPort; // asigna persistencia del evento
        this.clockPort = clockPort; // asigna reloj
        this.notificationMatrix = notificationMatrix; // asigna matriz de avisos
    }

    @Override
    public EndNavigationResult execute(EndNavigationCommand command) { // ejecuta el cierre completo en una transaccion
        Reservation reservation = repository.findById(command.reservationId()) // busca la reserva
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId())); // 404 si no existe

        validateIdentity(reservation, command); // solo el propietario puede cerrar la navegacion
        String remarks = normalizeRemarks(command.remarks()); // normaliza y valida las novedades
        validateState(reservation.getStatus()); // valida el estado antes de escribir
        ZoneId zone = resolveZone(reservation.getHarborTimezone()); // zona guardada en la reserva, sin llamar a M1
        Instant arrival = command.actualArrivalAt() == null
                ? clockPort.now()
                : command.actualArrivalAt().toInstant(); // usa la llegada informada o el reloj una sola vez

        reservation.setActualArrivalAt(arrival); // fija la llegada real en el agregado
        repository.save(reservation); // persiste la llegada antes de delegar el estado a CU-08

        OperationalStatus vesselOperationalStatus = notificationMatrix.decide( // calcula el aviso a M1
                ReservationStatus.IN_NAVIGATION,
                ReservationStatus.COMPLETED,
                false,
                reservation.isM1Blocked()).moduleOneStatus(); // estado operativo o null

        ChangeReservationStatusResult statusResult = changeReservationStatusUseCase.execute( // delega el cambio de estado
                new ChangeReservationStatusCommand(
                        command.reservationId(), // reserva
                        ReservationStatus.COMPLETED, // estado destino
                        null, // sin subtipo de cancelacion
                        ActorType.OWNER, // actor propietario
                        null, // sin motivo de auditoria
                        command.callerId(), // actor_id
                        null, // expectedVersion: la version ya subio en el paso anterior
                        false, // sin averia mecanica
                        null, // sin anticipacion
                        remarks, // notas de cierre
                        null, // sin referencia de pago
                        null)); // sin token de pago

        checkoutEventPort.save( // inserta el evento inmutable solo despues de CU-08
                command.reservationId(),
                command.callerId(),
                arrival,
                remarks);

        return new EndNavigationResult(
                command.reservationId(), // reserva
                statusResult.status(), // estado resultante
                arrival.atZone(zone).toOffsetDateTime(), // llegada con offset del puerto
                reservation.getVesselId(), // embarcacion
                vesselOperationalStatus, // aviso a M1 o null
                null, // disputa se conectara en CU-16
                null, // ventana de disputa se conectara en CU-16
                messageFor(vesselOperationalStatus, remarks)); // mensaje sin tildes
    }

    private void validateIdentity(Reservation reservation, EndNavigationCommand command) { // valida rol y pertenencia
        if (command.actor() != ActorType.OWNER || !reservation.isOwner(command.callerId())) { // solo el propietario titular
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only the reservation owner is authorized to end navigation"); // 403
        }
    }

    private String normalizeRemarks(String remarks) { // valida y normaliza las novedades
        if (remarks == null || remarks.isBlank()) { // ausente o solo espacios
            return null; // se trata como sin novedades
        }
        if (remarks.length() > MAX_REMARKS_LENGTH) { // supera el limite
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "remarks must not exceed 2000 characters"); // 400
        }
        return remarks; // conserva el texto original
    }

    private void validateState(ReservationStatus status) { // valida el estado antes de escribir
        if (status == ReservationStatus.COMPLETED) { // cierre repetido
            throw new ReservationRuleException(ErrorCode.TRIP_ALREADY_COMPLETED,
                    "Reservation is already completed"); // 409
        }
        if (status != ReservationStatus.IN_NAVIGATION) { // estado distinto del esperado
            throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "Reservation cannot be completed from status " + status); // 409
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

    private String messageFor(OperationalStatus vesselOperationalStatus, String remarks) { // selecciona el mensaje informativo
        if (vesselOperationalStatus != null) { // hubo aviso a M1
            return remarks == null ? MESSAGE_WITH_M1 : MESSAGE_WITH_M1_REMARKS; // con o sin novedades
        }
        return remarks == null ? MESSAGE_WITHOUT_M1 : MESSAGE_WITHOUT_M1_REMARKS; // sin aviso a M1
    }
}