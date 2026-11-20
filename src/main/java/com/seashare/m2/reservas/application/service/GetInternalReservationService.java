package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.application.port.in.GetInternalReservationUseCase;
import com.seashare.m2.reservas.application.port.out.InternalReservationReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional(readOnly = true) // consulta pura: no escribe ni emite eventos
public class GetInternalReservationService implements GetInternalReservationUseCase { // servicio de lectura para CU-15

    private static final String SYSTEM_TTL_ACTOR = "SYSTEM_TTL"; // actor reportado cuando expira el temporizador

    private final InternalReservationReadPort readPort; // puerto de lectura de la reserva
    private final ClockPort clockPort; // reloj inyectable para calcular el ttl

    public GetInternalReservationService(InternalReservationReadPort readPort, ClockPort clockPort) { // constructor con dependencias
        this.readPort = readPort; // asigna puerto de lectura
        this.clockPort = clockPort; // asigna reloj
    }

    @Override
    public InternalReservationView execute(UUID reservationId) { // consulta y enriquece los datos de la reserva
        InternalReservationView view = readPort.findById(reservationId) // busca la reserva por identificador
                .orElseThrow(() -> new ReservationNotFoundException(reservationId)); // falla si no existe

        int durationDays = (int) ChronoUnit.DAYS.between(view.startDate(), view.endDate()) + 1; // ultimo dia cuenta como ocupado
        int durationNights = durationDays - 1; // noches = dias menos uno, sin relevo el mismo dia

        return view.withDerived( // agrega los campos calculados en la capa de aplicacion
                durationDays,
                durationNights,
                resolveCancellationActor(view), // actor segun el estado final
                ttlApplies(view), // solo INITIATED y PENDING_PAYMENT llevan ttl
                remainingSeconds(view) // segundos restantes o cero si no aplica
        );
    }

    private String resolveCancellationActor(InternalReservationView view) { // decide el actor de cancelacion
        if (view.status() == ReservationStatus.CANCELLED) { // cancelacion activa: actor de la auditoria
            return view.latestCancellationActor(); // RENTER u OWNER segun quien la disparo
        }
        if (view.status() == ReservationStatus.EXPIRED) { // expiracion automatica por ttl
            return SYSTEM_TTL_ACTOR; // valor adicional requerido por el contrato de M3
        }
        return null; // cualquier otro estado no expone actor
    }

    private boolean ttlApplies(InternalReservationView view) { // determina si el ttl esta vigente
        boolean stateWithTtl = view.status() == ReservationStatus.INITIATED
                || view.status() == ReservationStatus.PENDING_PAYMENT; // estados del embudo de pago
        return stateWithTtl && view.ttlExpiresAt() != null; // requiere ademas una fecha de expiracion
    }

    private Long remainingSeconds(InternalReservationView view) { // calcula los segundos restantes del ttl
        if (!ttlApplies(view)) { // sin ttl vigente no se informa tiempo
            return 0L; // el contrato exige entero no nulo: cero cuando no aplica
        }
        Instant now = clockPort.now(); // hora actual siempre por el puerto inyectable
        long seconds = Duration.between(now, view.ttlExpiresAt()).getSeconds(); // diferencia en segundos
        return Math.max(0L, seconds); // nunca negativo, aunque el barrido aun no haya expirado la reserva
    }
}
