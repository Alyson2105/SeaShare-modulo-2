package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class StartPaymentWriter { // CU-03: frontera transaccional con bloqueo pesimista y transicion

    private static final String PAYMENT_REASON = "Pago iniciado por el arrendatario"; // motivo auditado

    private static final String LAST_REJECTED = "REJECTED"; // unico resultado que permite reintento

    private final ReservationRepositoryPort reservationRepository;

    private final ChangeReservationStatusUseCase changeReservationStatus;

    private final PaymentAttemptPort paymentAttemptPort;

    private final OutboxPort outboxPort;

    private final ClockPort clockPort;

    public StartPaymentWriter(ReservationRepositoryPort reservationRepository,
                              ChangeReservationStatusUseCase changeReservationStatus,
                              PaymentAttemptPort paymentAttemptPort,
                              OutboxPort outboxPort,
                              ClockPort clockPort) {
        this.reservationRepository = reservationRepository; // asigna repositorio
        this.changeReservationStatus = changeReservationStatus; // asigna motor CU-08
        this.paymentAttemptPort = paymentAttemptPort; // asigna historial de intentos
        this.outboxPort = outboxPort; // asigna outbox para el reintento
        this.clockPort = clockPort; // asigna reloj
    }

    @Transactional
    public StartPaymentResult startPayment(UUID reservationId,
                                           CalculatedValue calculation,
                                           String paymentTokenRef,
                                           Instant requestedAt) { // una sola transaccion para validar y avanzar
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId) // SELECT ... FOR UPDATE
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        reservationRepository.lockVesselForPayment(reservation.getVesselId()); // FCFS entre reservas distintas del mismo barco

        assertTtlActive(reservation, clockPort.now()); // el TTL puede vencer mientras espera el bloqueo

        if (reservation.getStatus() == ReservationStatus.INITIATED) {
            return firstPayment(reservation, calculation, paymentTokenRef, requestedAt);
        }
        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
            return retryPayment(reservation, paymentTokenRef, requestedAt);
        }
        throw new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                "Solo se puede iniciar el pago de reservas en estado Iniciada (estado actual: "
                        + reservation.getStatus() + ")");
    }

    private StartPaymentResult firstPayment(Reservation reservation,
                                            CalculatedValue calculation,
                                            String paymentTokenRef,
                                            Instant requestedAt) { // FR-005 y FR-006
        if (calculation == null) { // la primera vez exige el calculo de M3
            throw new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE,
                    "El calculo total es obligatorio para iniciar el pago");
        }
        if (reservationRepository.existsOverlap(reservation.getVesselId(), reservation.getStartDate(),
                reservation.getEndDate(), CreateReservationService.BLOCKING_STATUSES)) {
            throw new ReservationRuleException(ErrorCode.CONCURRENCY_CONFLICT,
                    "La embarcacion ya no se encuentra disponible para las fechas seleccionadas debido a un pago concurrente");
        }

        persistCalculation(reservation, calculation); // desglose literal de M3

        ChangeReservationStatusResult result = changeReservationStatus.execute(new ChangeReservationStatusCommand(
                reservation.getId(),
                ReservationStatus.PENDING_PAYMENT,
                null,
                ActorType.RENTER,
                PAYMENT_REASON,
                reservation.getRenterId(),
                null,
                null,
                null,
                null,
                null,
                paymentTokenRef)); // viaja en el evento PENDING (CU-14)

        return buildResult(reservation, calculation, result.ttlExpiresAt(), requestedAt);
    }

    private StartPaymentResult retryPayment(Reservation reservation,
                                            String paymentTokenRef,
                                            Instant requestedAt) { // nuevo intento tras un REJECTED
        String lastResult = paymentAttemptPort.lastResult(reservation.getId()).orElse(null);
        if (!LAST_REJECTED.equals(lastResult)) { // solo se reintenta tras un rechazo
            throw new ReservationRuleException(ErrorCode.RETRY_NOT_ALLOWED,
                    "Ya hay un cobro en proceso para esta reserva");
        }

        if (!paymentAttemptPort.claimLastRejectionForRetry(reservation.getId())) {
            throw new ReservationRuleException(ErrorCode.RETRY_NOT_ALLOWED,
                    "Ya se inicio un nuevo cobro para el ultimo rechazo");
        }

        CalculatedValue persisted = persistedCalculation(reservation); // se reutiliza el calculo ya asociado
        UUID eventId = UUID.randomUUID(); // nuevo intento: nuevo Message-Id
        outboxPort.enqueue(new StatusChangedEvent( // re-notifica PENDING con el token nuevo
                eventId,
                reservation.getId(),
                ReservationStatus.PENDING_PAYMENT,
                null,
                requestedAt,
                ActorType.RENTER,
                null,
                null,
                paymentTokenRef));

        return buildResult(reservation, persisted, reservation.getTtlExpiresAt(), requestedAt);
    }

    private StartPaymentResult buildResult(Reservation reservation,
                                           CalculatedValue calculation,
                                           Instant ttlExpiresAt,
                                           Instant requestedAt) {
        Instant expiresAt = ttlExpiresAt != null ? ttlExpiresAt : reservation.getTtlExpiresAt();
        long remaining = expiresAt == null ? 0L
                : Math.max(0L, Duration.between(clockPort.now(), expiresAt).getSeconds());
        int attempt = paymentAttemptPort.countAttempts(reservation.getId()) + 1; // primer intento o reintento

        return new StartPaymentResult(
                reservation.getId(),
                ReservationStatus.PENDING_PAYMENT,
                reservation.getVesselId(),
                calculation,
                remaining,
                expiresAt == null ? null
                        : expiresAt.atZone(HarborTime.zoneOf(reservation.getHarborTimezone())).toOffsetDateTime(),
                attempt);
    }

    private void persistCalculation(Reservation reservation, CalculatedValue calculation) { // sin aritmetica monetaria
        reservation.setRentalAmount(calculation.rentalAmount());
        reservation.setInsuranceAmount(calculation.insuranceAmount());
        reservation.setGuaranteeDepositAmount(calculation.guaranteeDepositAmount());
        reservation.setTotalAmount(calculation.totalAmount());
        reservationRepository.save(reservation); // persiste el desglose antes de la transicion
    }

    private CalculatedValue persistedCalculation(Reservation reservation) { // reutiliza el calculo del primer intento
        if (reservation.getRentalAmount() == null || reservation.getInsuranceAmount() == null
                || reservation.getGuaranteeDepositAmount() == null || reservation.getTotalAmount() == null) {
            throw new ReservationRuleException(ErrorCode.INTERNAL_ERROR,
                    "El desglose del primer intento no esta persistido");
        }
        return new CalculatedValue(reservation.getRentalAmount(), reservation.getInsuranceAmount(),
                reservation.getGuaranteeDepositAmount(), reservation.getTotalAmount());
    }

    private void assertTtlActive(Reservation reservation, Instant requestedAt) { // corte exacto
        if (reservation.getTtlExpiresAt() != null && !requestedAt.isBefore(reservation.getTtlExpiresAt())) {
            throw new ReservationRuleException(ErrorCode.RESERVATION_EXPIRED,
                    "El tiempo limite de 15 minutos para iniciar el pago de esta reserva ha expirado");
        }
    }
}
