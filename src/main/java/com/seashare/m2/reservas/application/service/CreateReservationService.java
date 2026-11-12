package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.InitiateReservationCommand;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.NewReservation;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.service.TtlPolicy;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateReservationService implements CreateReservationUseCase { // caso de uso de creacion; sin transaccion para no retener conexion ante M1

    public static final String IDEMPOTENCY_ENDPOINT = CreateReservationWriter.IDEMPOTENCY_ENDPOINT; // identificador del endpoint
    public static final Duration IDEMPOTENCY_WINDOW = Duration.ofSeconds(60); // ventana temporal de expiracion de clave de idempotencia
    public static final Set<ReservationStatus> BLOCKING_STATUSES = EnumSet.of( // estados que bloquean disponibilidad de fechas
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.IN_NAVIGATION
    );

    private final ClockPort clockPort; // puerto de reloj para marca temporal unica
    private final FleetInfoPort fleetInfoPort; // puerto para consulta de informacion de flota en modulo 1
    private final FleetOperationalStatusPort fleetOperationalStatusPort; // puerto para consultar disponibilidad operativa en modulo 1
    private final ReservationRepositoryPort reservationRepositoryPort; // puerto para verificacion de solapamiento
    private final CreateReservationWriter writer; // frontera transaccional de la escritura

    public CreateReservationService(ClockPort clockPort,
                                    FleetInfoPort fleetInfoPort,
                                    FleetOperationalStatusPort fleetOperationalStatusPort,
                                    ReservationRepositoryPort reservationRepositoryPort,
                                    CreateReservationWriter writer) { // inyeccion de dependencias requeridas
        this.clockPort = clockPort; // asigna puerto de reloj
        this.fleetInfoPort = fleetInfoPort; // asigna puerto de flota
        this.fleetOperationalStatusPort = fleetOperationalStatusPort; // asigna puerto de estado operativo
        this.reservationRepositoryPort = reservationRepositoryPort; // asigna repositorio
        this.writer = writer; // asigna escritura transaccional
    }

    @Override
    public ReservationView execute(CreateReservationCommand command) { // valida, consulta M1 fuera de transaccion y delega la escritura
        Instant now = clockPort.now(); // captura instante actual del sistema una sola vez

        if (command.endDate().isBefore(command.startDate())) { // valida consistencia de rango de fechas
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "End date cannot be before start date"); // rechaza rango cronologico invalido
        }

        String normalizedKey = UUID.fromString(command.idempotencyKey()).toString(); // normaliza representacion canonica del uuid

        FleetInfo fleetInfo = fleetInfoPort.getVesselInfo(command.vesselId()); // consulta M1 fuera de la transaccion

        if (fleetInfo.harborTimezone() == null || fleetInfo.harborTimezone().isBlank()) { // valida presencia de zona horaria
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE, "Harbor timezone is invalid or unavailable"); // rechaza zona horaria ausente
        }
        try {
            ZoneId.of(fleetInfo.harborTimezone()); // valida formato de zona horaria
        } catch (DateTimeException e) {
            throw new ReservationRuleException(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE, "Harbor timezone is invalid or unavailable"); // rechaza zona horaria invalida
        }

        if (!fleetOperationalStatusPort.isAvailable(command.vesselId())) { // valida aptitud operativa en modulo 1
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_AVAILABLE, "Vessel is not available for operation"); // rechaza embarcacion inhabilitada
        }

        if (command.passengers() > fleetInfo.maxCapacity()) { // valida limite de capacidad informada
            throw new ReservationRuleException(ErrorCode.CAPACITY_EXCEEDED, "Requested passengers exceed vessel maximum capacity"); // rechaza exceso de pasajeros
        }

        if (reservationRepositoryPort.existsOverlap(command.vesselId(), command.startDate(), command.endDate(), BLOCKING_STATUSES)) { // valida disponibilidad local
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

        Reservation created = writer.createOrReturnExisting( // reclama idempotencia y escribe en una sola transaccion
                normalizedKey, now, IDEMPOTENCY_WINDOW, new InitiateReservationCommand(newReservation, fleetInfo.maxCapacity())); // CU-08 crea, audita y encola
        return ReservationView.from(created); // retorna vista inmutable de la reserva creada
    }
}
