package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.ConfirmPaymentResult;
import com.seashare.m2.reservas.application.dto.LatePaymentReversalEvent;
import com.seashare.m2.reservas.application.dto.PaymentConfirmationOutcome;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.ConfirmPaymentUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class ConfirmPaymentService implements ConfirmPaymentUseCase { // CU-13: logica comun del canal A y B

    private static final Logger log = LoggerFactory.getLogger(ConfirmPaymentService.class);

    private static final String CONFIRMED_REASON = "Pago aprobado por Modulo 3"; // auditoria de RESERVED

    private static final String FAILED_REASON = "Cobro rechazado definitivamente por Modulo 3"; // auditoria de PAYMENT_FAILED

    private final ReservationRepositoryPort reservationRepository;

    private final ChangeReservationStatusUseCase changeReservationStatus;

    private final PaymentAttemptPort paymentAttemptPort;

    private final OutboxPort outboxPort;

    private final ClockPort clockPort;

    public ConfirmPaymentService(ReservationRepositoryPort reservationRepository,
                                 ChangeReservationStatusUseCase changeReservationStatus,
                                 PaymentAttemptPort paymentAttemptPort,
                                 OutboxPort outboxPort,
                                 ClockPort clockPort) {
        this.reservationRepository = reservationRepository;
        this.changeReservationStatus = changeReservationStatus;
        this.paymentAttemptPort = paymentAttemptPort;
        this.outboxPort = outboxPort;
        this.clockPort = clockPort;
    }

    @Override
    @Transactional
    public ConfirmPaymentResult confirm(ConfirmPaymentCommand command) { // una sola transaccion por notificacion
        if (command.result() == PaymentStatus.IN_PROCESS || command.result() == PaymentStatus.UNKNOWN) {
            return new ConfirmPaymentResult(PaymentConfirmationOutcome.IGNORED, null); // nunca se asume resultado
        }

        Reservation reservation = reservationRepository.findByIdForUpdate(command.reservationId()) // bloqueo pesimista
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));
        Instant now = clockPort.now();

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
            return processPending(command, reservation, now); // cobro en curso
        }
        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            boolean firstTime = register(command, reservation.getId(), now); // clave idempotente (FR-007)
            return new ConfirmPaymentResult(
                    firstTime ? PaymentConfirmationOutcome.IGNORED : PaymentConfirmationOutcome.DUPLICATE,
                    ReservationStatus.RESERVED); // una reserva ya consolidada no se vuelve a tocar
        }
        if (reservation.getStatus() == ReservationStatus.EXPIRED
                || reservation.getStatus() == ReservationStatus.PAYMENT_FAILED) {
            return processTerminal(command, reservation, now); // posible cobro huerfano
        }
        return new ConfirmPaymentResult(PaymentConfirmationOutcome.IGNORED, reservation.getStatus()); // ya consolidada
    }

    private ConfirmPaymentResult processPending(ConfirmPaymentCommand command, Reservation reservation, Instant now) {
        boolean ttlActive = reservation.getTtlExpiresAt() == null || now.isBefore(reservation.getTtlExpiresAt());

        if (command.result() == PaymentStatus.APPROVED) {
            if (ttlActive) { // FR-004: confirmacion dentro del TTL
                if (!register(command, reservation.getId(), now)) {
                    return duplicate(reservation); // notificacion repetida
                }
                changeReservationStatus.execute(new ChangeReservationStatusCommand(
                        reservation.getId(),
                        ReservationStatus.RESERVED,
                        null,
                        ActorType.SYSTEM,
                        CONFIRMED_REASON,
                        null,
                        null,
                        null,
                        null,
                        null,
                        command.externalTransactionId(),
                        null)); // payment_reference queda registrada en la reserva
                return new ConfirmPaymentResult(PaymentConfirmationOutcome.CONFIRMED, ReservationStatus.RESERVED);
            }
            return lateReversal(command, reservation, now); // el TTL ya vencio: reversion
        }

        if (command.result() == PaymentStatus.REJECTED) {
            if (!register(command, reservation.getId(), now)) {
                return duplicate(reservation);
            }
            return new ConfirmPaymentResult(PaymentConfirmationOutcome.PENDING_RETRY, ReservationStatus.PENDING_PAYMENT);
        }

        if (command.result() == PaymentStatus.CANCELLED || command.result() == PaymentStatus.EXPIRED) {
            if (!register(command, reservation.getId(), now)) {
                return duplicate(reservation);
            }
            changeReservationStatus.execute(new ChangeReservationStatusCommand(
                    reservation.getId(),
                    ReservationStatus.PAYMENT_FAILED,
                    null,
                    ActorType.SYSTEM,
                    FAILED_REASON,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
            return new ConfirmPaymentResult(PaymentConfirmationOutcome.PAYMENT_FAILED, ReservationStatus.PAYMENT_FAILED);
        }

        return new ConfirmPaymentResult(PaymentConfirmationOutcome.IGNORED, reservation.getStatus());
    }

    private ConfirmPaymentResult processTerminal(ConfirmPaymentCommand command, Reservation reservation, Instant now) {
        if (command.result() != PaymentStatus.APPROVED) {
            return new ConfirmPaymentResult(PaymentConfirmationOutcome.IGNORED, reservation.getStatus());
        }
        return lateReversal(command, reservation, now); // cobro aprobado sobre una reserva ya cerrada
    }

    private ConfirmPaymentResult lateReversal(ConfirmPaymentCommand command, Reservation reservation, Instant now) {
        if (!register(command, reservation.getId(), now)) {
            return duplicate(reservation); // la reversion ya se instruyo
        }
        UUID eventId = UUID.nameUUIDFromBytes( // id estable por reserva y transaccion
                ("late-payment-reversal:" + reservation.getId() + ":" + command.externalTransactionId())
                        .getBytes(StandardCharsets.UTF_8));
        outboxPort.enqueue(new LatePaymentReversalEvent(eventId, reservation.getId(), now)); // CANCELLED_FLEXIBLE
        log.error("ALERTA CU-13: cobro aprobado fuera del TTL para la reserva {}; reversion instruida (transaccion {})",
                reservation.getId(), command.externalTransactionId());
        return new ConfirmPaymentResult(PaymentConfirmationOutcome.LATE_REVERSAL, reservation.getStatus());
    }

    private boolean register(ConfirmPaymentCommand command, UUID reservationId, Instant now) { // idempotencia estricta
        String reference = command.externalTransactionId() == null
                ? "m3-" + reservationId + "-" + command.result().name() // referencia sintetica estable
                : command.externalTransactionId();
        return paymentAttemptPort.registerIfAbsent(
                reservationId, reference, command.result().name(), command.detail(), now);
    }

    private ConfirmPaymentResult duplicate(Reservation reservation) {
        return new ConfirmPaymentResult(PaymentConfirmationOutcome.DUPLICATE, reservation.getStatus());
    }
}
