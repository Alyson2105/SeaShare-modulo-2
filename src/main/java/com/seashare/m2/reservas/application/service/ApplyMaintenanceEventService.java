package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.MaintenanceEventCommand;
import com.seashare.m2.reservas.application.dto.MaintenanceEventOutcome;
import com.seashare.m2.reservas.application.dto.MaintenanceEventResult;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.ApplyMaintenanceEventUseCase;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationCause;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Aplica un aviso de mantenimiento de Modulo 1 (contrato C5, {@code consistencia-m2-m1.md} §6).
 *
 * <p>No es transaccional a proposito: cada cancelacion se ejecuta en su propia transaccion a
 * traves de CU-08. Un fallo parcial no guarda idempotencia y responde 503 para que M1 reintente.</p>
 */
@Service
public class ApplyMaintenanceEventService implements ApplyMaintenanceEventUseCase { // orquestador de C5

    static final String IDEMPOTENCY_ENDPOINT = "/api/v1/internal/maintenance-events"; // clave de idempotencia (B.2)

    static final String MAINTENANCE_REASON = "MAINTENANCE"; // motivo auditable de la cancelacion

    private static final Logger log = LoggerFactory.getLogger(ApplyMaintenanceEventService.class); // trazas del proceso

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Bogota"); // zona por defecto del contrato C3

    private static final Set<String> ALLOWED_REASONS = Set.of("OWNER", "ADMIN"); // motivo informativo del aviso

    private static final int MAX_EVENT_ID = 100; // limite del contrato

    private final ReservationRepositoryPort repository; // busqueda de reservas afectadas

    private final ChangeReservationStatusUseCase changeReservationStatus; // unico escritor de estado

    private final IdempotencyPort idempotencyPort; // deduplicacion por event_id

    private final ClockPort clockPort; // reloj unico

    private final boolean autoCancel; // m2.maintenance.auto-cancel

    public ApplyMaintenanceEventService(ReservationRepositoryPort repository,
                                        ChangeReservationStatusUseCase changeReservationStatus,
                                        IdempotencyPort idempotencyPort,
                                        ClockPort clockPort,
                                        @Value("${m2.maintenance.auto-cancel:true}") boolean autoCancel) {
        this.repository = repository; // asigna repositorio
        this.changeReservationStatus = changeReservationStatus; // asigna CU-08
        this.idempotencyPort = idempotencyPort; // asigna idempotencia
        this.clockPort = clockPort; // asigna reloj
        this.autoCancel = autoCancel; // asigna bandera de cancelacion automatica
    }

    @Override
    public MaintenanceEventResult apply(MaintenanceEventCommand command) { // flujo completo de C5
        Instant now = clockPort.now(); // instante unico del aviso
        ParsedEvent parsed = parse(command, now); // valida campos y rango (400 si procede)

        Optional<String> existing = idempotencyPort.findOutcome(parsed.eventId(), IDEMPOTENCY_ENDPOINT); // reenvio
        if (existing.isPresent()) { // event_id ya procesado por completo
            return new MaintenanceEventResult(parsed.eventId(), MaintenanceEventOutcome.DUPLICATE); // no se vuelve a cancelar
        }

        if (!idempotencyPort.tryClaimPermanent(parsed.eventId(), IDEMPOTENCY_ENDPOINT, now)) { // otro proceso lo tiene
            Optional<String> afterClaim = idempotencyPort.findOutcome(parsed.eventId(), IDEMPOTENCY_ENDPOINT);
            if (afterClaim.isPresent()) { // se completo entre la consulta y el reclamo
                return new MaintenanceEventResult(parsed.eventId(), MaintenanceEventOutcome.DUPLICATE);
            }
            throw new ReservationRuleException(ErrorCode.MAINTENANCE_EVENT_PARTIAL_FAILURE,
                    "Maintenance event is already being processed"); // M1 reintenta mas tarde
        }

        try {
            MaintenanceEventOutcome outcome = process(parsed, now); // aplica segun el contrato
            idempotencyPort.completeWithOutcome(parsed.eventId(), IDEMPOTENCY_ENDPOINT, outcome.name()); // guarda outcome
            return new MaintenanceEventResult(parsed.eventId(), outcome);
        } catch (RuntimeException failure) { // 503, 500 o fallo inesperado
            idempotencyPort.releaseClaim(parsed.eventId(), IDEMPOTENCY_ENDPOINT); // el reintento debe reprocesar
            throw failure;
        }
    }

    private MaintenanceEventOutcome process(ParsedEvent event, Instant now) { // reglas del contrato §6
        if (!repository.existsAnyByVesselId(event.vesselId())) { // M2 no tiene historial local
            return MaintenanceEventOutcome.UNKNOWN_VESSEL;
        }
        if (event.maintenanceUntil().isBefore(now)) { // rango integramente en el pasado
            return MaintenanceEventOutcome.INVALID_RANGE;
        }
        if (!autoCancel) { // bandera de configuracion
            return MaintenanceEventOutcome.AUTO_CANCEL_DISABLED;
        }

        LocalDate from = event.maintenanceStart().atZone(DEFAULT_ZONE).toLocalDate().minusDays(1); // margen por zona
        LocalDate to = event.maintenanceUntil().atZone(DEFAULT_ZONE).toLocalDate().plusDays(1); // margen por zona
        List<UUID> candidates = repository.findReservedByVesselOverlapping(event.vesselId(), from, to); // RESERVED que cruzan
        List<UUID> affected = candidates.stream()
                .filter(id -> crosses(id, event)) // verificacion con la zona real de cada reserva
                .toList();
        if (affected.isEmpty()) { // ninguna reserva afectada
            return MaintenanceEventOutcome.NO_AFFECTED_RESERVATIONS;
        }

        int failures = 0; // cancelaciones fallidas
        for (UUID reservationId : affected) { // una transaccion por reserva
            try {
                changeReservationStatus.execute(cancellation(reservationId)); // CU-08 con causa MAINTENANCE
            } catch (RuntimeException failure) { // continua con las demas
                failures++;
                log.warn("Maintenance cancellation failed for reservation {}", reservationId, failure);
            }
        }
        if (failures > 0) { // fallo parcial: no se guarda idempotencia
            throw new ReservationRuleException(ErrorCode.MAINTENANCE_EVENT_PARTIAL_FAILURE,
                    failures + " de " + affected.size() + " reservas no se pudieron cancelar");
        }
        return MaintenanceEventOutcome.APPLIED; // al menos una reserva cancelada
    }

    private boolean crosses(UUID reservationId, ParsedEvent event) { // cruce inclusivo en la zona del puerto
        Optional<Reservation> found = repository.findById(reservationId);
        if (found.isEmpty()) { // la fila desaparecio entre la consulta y la lectura
            return false;
        }
        Reservation reservation = found.get();
        ZoneId zone = zoneOf(reservation.getHarborTimezone());
        LocalDate maintenanceStart = event.maintenanceStart().atZone(zone).toLocalDate();
        LocalDate maintenanceEnd = event.maintenanceUntil().atZone(zone).toLocalDate();
        return !maintenanceStart.isAfter(reservation.getEndDate())
                && !maintenanceEnd.isBefore(reservation.getStartDate()); // cualquier dia tocado queda bloqueado
    }

    private ChangeReservationStatusCommand cancellation(UUID reservationId) { // comando acotado de C5
        return new ChangeReservationStatusCommand(
                reservationId,
                ReservationStatus.CANCELLED,
                CancellationSubtype.BY_OWNER,
                ActorType.SYSTEM,
                MAINTENANCE_REASON,
                null, // actor_id nulo para SYSTEM
                null, // sin version esperada
                null, // sin averia mecanica
                null, // sin anticipacion
                null, // sin notas
                null, // sin referencia de pago
                null, // sin token
                null, // sin medio de pago
                null, // sin correo
                null, // sin ultimos cuatro
                CancellationCause.MAINTENANCE); // causa acotada
    }

    private ParsedEvent parse(MaintenanceEventCommand command, Instant now) { // validacion previa al procesamiento
        String eventId = command == null || command.eventId() == null ? "" : command.eventId().trim();
        if (eventId.isEmpty() || eventId.length() > MAX_EVENT_ID) { // clave obligatoria y acotada
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "event_id is required and must not exceed 100 characters");
        }

        UUID vesselId;
        try {
            vesselId = UUID.fromString(command.vesselId() == null ? "" : command.vesselId().trim());
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "vessel_id must be a valid UUID");
        }

        Instant occurredAt = requiredInstant(command.occurredAt(), "occurred_at"); // momento del cambio
        Instant maintenanceUntil = requiredInstant(command.maintenanceUntil(), "maintenance_until"); // fin obligatorio
        Instant maintenanceStart = optionalInstant(command.maintenanceStart(), "maintenance_start"); // inicio opcional
        Instant effectiveStart = maintenanceStart == null ? now : maintenanceStart; // nulo = ahora

        if (!maintenanceUntil.isAfter(effectiveStart)) { // rango incoherente
            throw new ReservationRuleException(ErrorCode.INVALID_MAINTENANCE_RANGE,
                    "maintenance_until must be after maintenance_start");
        }

        String reason = normalizeReason(command.reason()); // OWNER o ADMIN, informativo
        return new ParsedEvent(eventId, vesselId, occurredAt, effectiveStart, maintenanceUntil, reason);
    }

    private Instant requiredInstant(String value, String field) { // fecha obligatoria con offset
        if (value == null || value.isBlank()) { // ausente
            if ("maintenance_until".equals(field)) { // D-05: until es obligatorio
                throw new ReservationRuleException(ErrorCode.INVALID_MAINTENANCE_RANGE,
                        "maintenance_until is required");
            }
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, field + " is required");
        }
        return parseInstant(value, field); // formato ISO-8601 con offset
    }

    private Instant optionalInstant(String value, String field) { // fecha opcional con offset
        if (value == null || value.isBlank()) { // ausente
            return null; // se interpreta como ahora
        }
        return parseInstant(value, field); // formato ISO-8601 con offset
    }

    private Instant parseInstant(String value, String field) { // parseo comun
        try {
            return OffsetDateTime.parse(value.trim()).toInstant(); // exige offset explicito
        } catch (DateTimeParseException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    field + " must be an ISO-8601 date-time with offset");
        }
    }

    private String normalizeReason(String reason) { // valida el motivo informativo
        if (reason == null || reason.isBlank()) { // opcional
            return null;
        }
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_REASONS.contains(normalized)) { // valor fuera del catalogo
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "reason must be OWNER or ADMIN");
        }
        return normalized;
    }

    private ZoneId zoneOf(String timezone) { // zona del puerto con valor por defecto
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            return DEFAULT_ZONE; // reserva historica con zona ilegible
        }
    }

    private record ParsedEvent(String eventId,
                               UUID vesselId,
                               Instant occurredAt,
                               Instant maintenanceStart,
                               Instant maintenanceUntil,
                               String reason) { // aviso validado
    }
}
