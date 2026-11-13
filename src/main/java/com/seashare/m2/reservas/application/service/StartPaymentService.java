package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
import com.seashare.m2.reservas.application.port.in.StartPaymentUseCase;
import com.seashare.m2.reservas.application.port.out.CalculatedValuePort;
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

import java.time.Instant;

@Service
public class StartPaymentService implements StartPaymentUseCase { // CU-03: orquesta consultas externas y transicion

    private static final String RENTER_ROLE = "RENTER"; // perfil autorizado

    private static final String LAST_REJECTED = "REJECTED"; // unico resultado que permite reintento

    private final ReservationRepositoryPort reservationRepository; // lectura y pertenencia

    private final CalculatedValuePort calculatedValuePort; // calculo final de M3 (CU-12)

    private final FleetOperationalStatusPort fleetOperationalStatusPort; // estado fisico de M1 (CU-10)

    private final PaymentAttemptPort paymentAttemptPort; // ultimo resultado del cobro

    private final StartPaymentWriter writer; // frontera transaccional con lock

    private final ClockPort clockPort; // reloj unico

    public StartPaymentService(ReservationRepositoryPort reservationRepository,
                               CalculatedValuePort calculatedValuePort,
                               FleetOperationalStatusPort fleetOperationalStatusPort,
                               PaymentAttemptPort paymentAttemptPort,
                               StartPaymentWriter writer,
                               ClockPort clockPort) {
        this.reservationRepository = reservationRepository; // asigna repositorio
        this.calculatedValuePort = calculatedValuePort; // asigna M3
        this.fleetOperationalStatusPort = fleetOperationalStatusPort; // asigna M1
        this.paymentAttemptPort = paymentAttemptPort; // asigna historial
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
        CalculatedValue calculation = calculatedValuePort.getCalculatedValue(command.reservationId()); // FR-003 fuera de TX

        if (!fleetOperationalStatusPort.isAvailable(reservation.getVesselId())) { // FR-005 fuera de TX
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_AVAILABLE,
                    "La embarcacion ya no se encuentra disponible para las fechas seleccionadas");
        }

        return writer.startPayment(command.reservationId(), calculation, command.paymentTokenRef(), now);
    }

    private StartPaymentResult retryPayment(Reservation reservation, StartPaymentCommand command, Instant now) {
        String lastResult = paymentAttemptPort.lastResult(reservation.getId()).orElse(null);
        if (!LAST_REJECTED.equals(lastResult)) { // solo tras un rechazo definitivo del cobro
            throw new ReservationRuleException(ErrorCode.RETRY_NOT_ALLOWED,
                    "Ya hay un cobro en proceso para esta reserva");
        }
        return writer.startPayment(command.reservationId(), null, command.paymentTokenRef(), now); // reutiliza el calculo
    }

    private void assertTtlActive(Reservation reservation, Instant now) { // TTL heredado de INITIATED
        if (reservation.getTtlExpiresAt() != null && !now.isBefore(reservation.getTtlExpiresAt())) {
            throw new ReservationRuleException(ErrorCode.RESERVATION_EXPIRED,
                    "El tiempo limite de 15 minutos para iniciar el pago de esta reserva ha expirado"); // 409
        }
    }
}
