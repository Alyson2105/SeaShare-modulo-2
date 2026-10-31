package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.NewReservation;
import com.seashare.m2.reservas.application.port.out.ReservationCreationPort;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.service.TtlPolicy;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateReservationService implements CreateReservationUseCase { // servicio de aplicacion para creacion formal de reservas

    public static final String IDEMPOTENCY_ENDPOINT = "CREATE_RESERVATION"; // identificador del endpoint para control de idempotencia
    public static final Duration IDEMPOTENCY_WINDOW = Duration.ofSeconds(60); // ventana temporal de expiracion de clave de idempotencia
    public static final Set<ReservationStatus> BLOCKING_STATUSES = EnumSet.of( // estados que bloquean disponibilidad de fechas
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.IN_NAVIGATION
    );

    private final ClockPort clockPort; // puerto de reloj para marca temporal unica
    private final IdempotencyPort idempotencyPort; // puerto para garantizar atomicidad e idempotencia
    private final FleetInfoPort fleetInfoPort; // puerto para consulta de informacion de flota en modulo 1
    private final FleetOperationalStatusPort fleetOperationalStatusPort; // puerto para consultar disponibilidad operativa en modulo 1
    private final ReservationCreationPort reservationCreationPort; // puerto para verificacion de solapamiento y persistencia

    public CreateReservationService(ClockPort clockPort,
                                    IdempotencyPort idempotencyPort,
                                    FleetInfoPort fleetInfoPort,
                                    FleetOperationalStatusPort fleetOperationalStatusPort,
                                    ReservationCreationPort reservationCreationPort) { // inyeccion de dependencias requeridas
        this.clockPort = clockPort; // asigna puerto de reloj
        this.idempotencyPort = idempotencyPort; // asigna puerto de idempotencia
        this.fleetInfoPort = fleetInfoPort; // asigna puerto de flota
        this.fleetOperationalStatusPort = fleetOperationalStatusPort; // asigna puerto de estado operativo
        this.reservationCreationPort = reservationCreationPort; // asigna puerto de creacion
    }

    @Override
    public ReservationView execute(CreateReservationCommand command) { // ejecuta el flujo de creacion bajo transaccion
        Instant now = clockPort.now(); // captura instante actual del sistema una sola vez

        if (command.endDate().isBefore(command.startDate())) { // valida consistencia de rango de fechas
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "End date cannot be before start date"); // rechaza rango cronologico invalido
        }

        String normalizedKey = UUID.fromString(command.idempotencyKey()).toString(); // normaliza representacion canonica del uuid

        if (!idempotencyPort.tryClaim(normalizedKey, IDEMPOTENCY_ENDPOINT, now, IDEMPOTENCY_WINDOW)) { // intenta reclamar clave dentro de la ventana
            UUID reservationId = idempotencyPort.findReservationId(normalizedKey, IDEMPOTENCY_ENDPOINT) // consulta id de reserva existente
                    .orElseThrow(() -> new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Existing idempotent reservation id not found")); // falla si no hay id
            return reservationCreationPort.findViewById(reservationId) // recupera la reserva original
                    .orElseThrow(() -> new ReservationRuleException(ErrorCode.INTERNAL_ERROR, "Existing idempotent reservation not found")); // retorna reserva original sin duplicar
        }

        FleetInfo fleetInfo = fleetInfoPort.getVesselInfo(command.vesselId()); // consulta datos de flota propagando posibles excepciones

        if (fleetInfo.harborTimezone() == null || fleetInfo.harborTimezone().isBlank()) { // el plan no permite zona horaria por defecto
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE, "Harbor timezone is missing or invalid"); // rechaza zona ausente
        }
        try {
            ZoneId.of(fleetInfo.harborTimezone()); // valida formato de zona horaria
        } catch (DateTimeException e) {
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE, "Harbor timezone is missing or invalid"); // rechaza zona invalida
        }

        if (!fleetOperationalStatusPort.isAvailable(command.vesselId())) { // valida aptitud operativa en modulo 1
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_AVAILABLE, "Vessel is not available for operation"); // rechaza embarcacion inhabilitada
        }

        if (command.passengers() > fleetInfo.maxCapacity()) { // valida limite de capacidad informada
            throw new ReservationRuleException(ErrorCode.CAPACITY_EXCEEDED, "Requested passengers exceed vessel maximum capacity"); // rechaza exceso de pasajeros
        }

        if (reservationCreationPort.existsOverlap(command.vesselId(), command.startDate(), command.endDate(), BLOCKING_STATUSES)) { // valida disponibilidad local
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_AVAILABLE, "Vessel has overlapping active reservations for given dates"); // rechaza solapamiento
        }

        Instant ttlExpiresAt = new TtlPolicy().expiresAt(now); // calcula expiracion fija a quince minutos

        NewReservation newReservation = new NewReservation( // construye datos inmutables de la nueva reserva
                command.renterId(), // arrendatario
                command.vesselId(), // embarcacion
                fleetInfo.ownerId(), // propietario provisto por modulo 1
                command.startDate(), // fecha de inicio
                command.endDate(), // fecha de fin
                command.passengers(), // pasajeros
                command.holderName(), // nombre del titular
                command.holderPhone(), // telefono del titular
                command.holderEmail(), // correo del titular
                command.estimatedTotal(), // total estimado sin operaciones
                command.departureTime(), // hora de salida pactada
                fleetInfo.harborTimezone(), // zona horaria del puerto
                now, // marca temporal de creacion
                ttlExpiresAt // marca temporal de expiracion
        );

        ReservationView createdReservation = reservationCreationPort.create(newReservation); // persiste registro formalmente
        idempotencyPort.complete(normalizedKey, IDEMPOTENCY_ENDPOINT, createdReservation.reservationId()); // asocia reserva a la clave reclamada

        return createdReservation; // retorna vista inmutable de la reserva creada
    }
}
