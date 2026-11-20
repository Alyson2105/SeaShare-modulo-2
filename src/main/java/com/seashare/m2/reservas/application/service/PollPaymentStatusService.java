package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.PaymentStatusResult;
import com.seashare.m2.reservas.application.port.in.ConfirmPaymentUseCase;
import com.seashare.m2.reservas.application.port.in.PollPaymentStatusUseCase;
import com.seashare.m2.reservas.application.port.out.PaymentStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.shared.domain.ClockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PollPaymentStatusService implements PollPaymentStatusUseCase { // CU-13 canal B: alimenta la misma logica

    private static final Logger log = LoggerFactory.getLogger(PollPaymentStatusService.class);

    private final ReservationRepositoryPort reservationRepository;

    private final PaymentStatusPort paymentStatusPort;

    private final ConfirmPaymentUseCase confirmPayment;

    private final ClockPort clockPort;

    private final int batchSize;

    private final Duration recentWindow;

    public PollPaymentStatusService(ReservationRepositoryPort reservationRepository,
                                    PaymentStatusPort paymentStatusPort,
                                    ConfirmPaymentUseCase confirmPayment,
                                    ClockPort clockPort,
                                    @Value("${m2.scheduler.payments.batch-size:50}") int batchSize,
                                    @Value("${m2.scheduler.payments.recent-window-minutes:30}") long recentWindowMinutes) {
        this.reservationRepository = reservationRepository;
        this.paymentStatusPort = paymentStatusPort;
        this.confirmPayment = confirmPayment;
        this.clockPort = clockPort;
        this.batchSize = batchSize;
        this.recentWindow = Duration.ofMinutes(recentWindowMinutes);
    }

    @Override
    public int execute() { // un ciclo de consulta
        Instant now = clockPort.now();
        List<UUID> candidates = reservationRepository.findPaymentCandidates(now.minus(recentWindow), batchSize);
        int processed = 0;
        for (UUID reservationId : candidates) {
            try {
                PaymentStatusResult status = paymentStatusPort.getStatus(reservationId);
                if (status == null || status.status() == PaymentStatus.IN_PROCESS
                        || status.status() == PaymentStatus.UNKNOWN) {
                    continue; // sin resultado definitivo: se reintenta en el siguiente ciclo
                }
                confirmPayment.confirm(new ConfirmPaymentCommand(
                        reservationId,
                        status.externalReference(),
                        status.status(),
                        clockPort.now(),
                        status.detail()));
                processed++;
            } catch (Exception e) { // un fallo no frena el ciclo ni propaga al job
                log.error("CU-13: fallo al procesar el cobro de la reserva {}: {}", reservationId, e.getMessage(), e);
            }
        }
        return processed;
    }
}
