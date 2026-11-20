package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.dto.CalculationAuditEntry;
import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
import com.seashare.m2.reservas.application.port.in.StartPaymentUseCase;
import com.seashare.m2.reservas.application.port.out.CalculatedValuePort;
import com.seashare.m2.reservas.application.port.out.CalculationAuditPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

@Service
public class StartPaymentService implements StartPaymentUseCase { // CU-03: orquesta consultas externas y transicion

    private static final String RENTER_ROLE = "RENTER"; // perfil autorizado

    private static final String LAST_REJECTED = "REJECTED"; // unico resultado que permite reintento

    private final ReservationRepositoryPort reservationRepository; // lectura y pertenencia

    private final CalculatedValuePort calculatedValuePort; // calculo final de M3 (CU-12)

    private final FleetOperationalStatusPort fleetOperationalStatusPort; // estado fisico de M1 (CU-10)

    private final PaymentAttemptPort paymentAttemptPort; // ultimo resultado del cobro

    private final CalculationAuditPort calculationAuditPort; // asiento auditable de FR-010

    private final StartPaymentWriter writer; // frontera transaccional con lock

    private final ClockPort clockPort; // reloj unico

    public StartPaymentService(ReservationRepositoryPort reservationRepository,
                               CalculatedValuePort calculatedValuePort,
                               FleetOperationalStatusPort fleetOperationalStatusPort,
                               PaymentAttemptPort paymentAttemptPort,
                               CalculationAuditPort calculationAuditPort,
                               StartPaymentWriter writer,
                               ClockPort clockPort) {
        this.reservationRepository = reservationRepository; // asigna repositorio
        this.calculatedValuePort = calculatedValuePort; // asigna M3
        this.fleetOperationalStatusPort = fleetOperationalStatusPort; // asigna M1
        this.paymentAttemptPort = paymentAttemptPort; // asigna historial
        this.calculationAuditPort = calculationAuditPort; // asigna auditoria de calculo
        this.writer = writer; // asigna escritura transaccional
        this.clockPort = clockPort; // asigna reloj
    }

    @Override
    public StartPaymentResult start(StartPaymentCommand command) { // flujo completo
        if (!RENTER_ROLE.equalsIgnoreCase(command.role())) { // solo el arrendatario paga
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Este recurso requiere perfil Arrendatario");
        }

        Instant now = clockPort.now(); // instante unico de la operacion
        Reservation reservation = reservationRepository.findById(command.reservationId()) // lectura fuera de transaccion
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        if (!reservation.isRenter(command.requesterId())) { // FR-001: solo el titular
            throw new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED,
                    "No tienes autorizacion para iniciar el pago de esta reserva");
        }
        assertTtlActive(reservation, now); // Edge Case TTL: corte exacto

        if (reservation.getStatus() == ReservationStatus.INITIATED) {
            return firstPayment(reservation, command, now);
        }
        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
            return retryPayment(reservation, command, now);
        }
        throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                "Solo se puede iniciar el pago de reservas en estado Iniciada (estado actual: "
                        + reservation.getStatus() + ")");
    }

    private StartPaymentResult firstPayment(Reservation reservation, StartPaymentCommand command, Instant now) {
        CalculatedValue calculation; // desglose recibido de M3
        try {
            calculation = calculatedValuePort.getCalculatedValue(command.reservationId()); // FR-003 fuera de TX
        } catch (RuntimeException failure) { // fallo de calculo o de integracion
            calculationAuditPort.record(new CalculationAuditEntry( // FR-010: se audita el intento fallido
                    command.reservationId(), null, null, null, null, null, "FAILED", now));
            throw failure; // conserva el error original de M3
        }
        calculationAuditPort.record(new CalculationAuditEntry( // FR-010: montos recibidos y estado del calculo
                command.reservationId(), null, calculation.rentalAmount(), calculation.insuranceAmount(),
                calculation.guaranteeDepositAmount(), calculation.totalAmount(), "SUCCESS", now));

        if (!fleetOperationalStatusPort.isAvailable(reservation.getVesselId(), // FR-005 fuera de TX
                reservation.getStartDate(), reservation.getEndDate(), harborZone(reservation))) { // rango y zona del puerto
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_AVAILABLE,
                    "La embarcacion no se encuentra disponible para operar"); // mensaje valido tambien para mantenimiento
        }

        return writer.startPayment(command.reservationId(), calculation, command.paymentTokenRef(), // pasa los tres datos a la frontera
                command.paymentMethodType(), command.payerEmail(), command.lastFour(), now);
    }

    private StartPaymentResult retryPayment(Reservation reservation, StartPaymentCommand command, Instant now) {
        String lastResult = paymentAttemptPort.lastResult(reservation.getId()).orElse(null);
        if (!LAST_REJECTED.equals(lastResult)) { // solo tras un rechazo definitivo del cobro
            throw new ReservationRuleException(ErrorCode.RETRY_NOT_ALLOWED,
                    "Ya hay un cobro en proceso para esta reserva");
        }
        return writer.startPayment(command.reservationId(), null, command.paymentTokenRef(), // reintento con los datos nuevos
                command.paymentMethodType(), command.payerEmail(), command.lastFour(), now); // reutiliza el calculo
    }

    private void assertTtlActive(Reservation reservation, Instant now) { // TTL heredado de INITIATED
        if (reservation.getTtlExpiresAt() != null && !now.isBefore(reservation.getTtlExpiresAt())) {
            throw new ReservationRuleException(ErrorCode.RESERVATION_EXPIRED,
                    "El tiempo limite de 15 minutos para iniciar el pago de esta reserva ha expirado"); // 409
        }
    }

    private ZoneId harborZone(Reservation reservation) { // zona del puerto guardada en la reserva, con valor por defecto del contrato
        String timezone = reservation.getHarborTimezone(); // zona persistida al crear la reserva
        if (timezone == null || timezone.isBlank()) { // reserva historica sin zona
            return ZoneId.of("America/Bogota"); // valor por defecto de C3
        }
        try {
            return ZoneId.of(timezone.trim()); // zona IANA valida
        } catch (DateTimeException e) {
            return ZoneId.of("America/Bogota"); // zona ilegible: misma politica que CU-10 (fail-safe de fechas)
        }
    }
}
