package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.ConfirmPaymentResult;
import com.seashare.m2.reservas.application.dto.LatePaymentReversalEvent;
import com.seashare.m2.reservas.application.dto.PaymentConfirmationOutcome;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmPaymentServiceTest { // CU-13: cada rama de la confirmacion de cobro

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private static final UUID RESERVATION_ID = UUID.randomUUID();

    private static final String REFERENCE = "pg-8841";

    @Mock
    private ReservationRepositoryPort reservationRepository;

    @Mock
    private ChangeReservationStatusUseCase changeReservationStatus;

    @Mock
    private PaymentAttemptPort paymentAttemptPort;

    @Mock
    private OutboxPort outboxPort;

    @Mock
    private ClockPort clockPort;

    private ConfirmPaymentService service;

    @BeforeEach
    void setUp() {
        service = new ConfirmPaymentService(reservationRepository, changeReservationStatus,
                paymentAttemptPort, outboxPort, clockPort);
    }

    private Reservation reservation(ReservationStatus status, Instant ttl) {
        return Reservation.builder()
                .id(RESERVATION_ID)
                .renterId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .vesselId(UUID.randomUUID())
                .startDate(LocalDate.of(2027, 6, 1))
                .endDate(LocalDate.of(2027, 6, 3))
                .passengers(2)
                .holderName("Juan")
                .holderPhone("300")
                .holderEmail("juan@example.com")
                .estimatedTotal(new BigDecimal("100.00"))
                .departureTime(LocalTime.of(9, 0))
                .harborTimezone("America/Bogota")
                .ttlExpiresAt(ttl)
                .createdAt(NOW)
                .status(status)
                .version(0L)
                .build();
    }

    private ConfirmPaymentCommand command(PaymentStatus result) {
        return new ConfirmPaymentCommand(RESERVATION_ID, REFERENCE, result, NOW, "detail");
    }

    @Test
    void inProcessIsIgnoredWithoutReadingTheReservation() { // nunca se asume un resultado
        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.IN_PROCESS));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.IGNORED);
        verify(reservationRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void approvedWithinTtlConfirmsReservation() { // FR-004
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.APPROVED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.CONFIRMED);
        ArgumentCaptor<ChangeReservationStatusCommand> captor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatus).execute(captor.capture());
        assertThat(captor.getValue().targetStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(captor.getValue().paymentReference()).isEqualTo(REFERENCE); // trazabilidad FR-004
        verify(outboxPort, never()).enqueue(any(LatePaymentReversalEvent.class));
    }

    @Test
    void approvedAfterTtlInstructsReversalWithoutChangingState() { // FR-006 y CU-13 plan L25
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW)));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.APPROVED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.LATE_REVERSAL);
        assertThat(result.status()).isEqualTo(ReservationStatus.PENDING_PAYMENT); // el TTL job la expira
        ArgumentCaptor<LatePaymentReversalEvent> captor = ArgumentCaptor.forClass(LatePaymentReversalEvent.class);
        verify(outboxPort).enqueue(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(RESERVATION_ID);
        verify(changeReservationStatus, never()).execute(any());
    }

    @Test
    void approvedOnExpiredReservationInstructsReversal() { // plan.external.md L123
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.EXPIRED, NOW.minusSeconds(60))));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.APPROVED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.LATE_REVERSAL);
        assertThat(result.status()).isEqualTo(ReservationStatus.EXPIRED); // no cambia
        verify(outboxPort).enqueue(any(LatePaymentReversalEvent.class));
    }

    @Test
    void rejectedWithTtlKeepsPendingPayment() { // FR-005: permite reintento
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.REJECTED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.PENDING_RETRY);
        verify(changeReservationStatus, never()).execute(any());
        verify(outboxPort, never()).enqueue(any(LatePaymentReversalEvent.class));
    }

    @Test
    void cancelledChargeFailsTheReservation() { // CANCELLED/EXPIRED de M3 -> PAYMENT_FAILED
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.CANCELLED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.PAYMENT_FAILED);
        ArgumentCaptor<ChangeReservationStatusCommand> captor =
                ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeReservationStatus).execute(captor.capture());
        assertThat(captor.getValue().targetStatus()).isEqualTo(ReservationStatus.PAYMENT_FAILED);
    }

    @Test
    void duplicateKeyHasNoEffects() { // FR-007
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(false);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.APPROVED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.DUPLICATE);
        verify(changeReservationStatus, never()).execute(any());
        verify(outboxPort, never()).enqueue(any(LatePaymentReversalEvent.class));
    }

    @Test
    void missingReservationIs404() {
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(command(PaymentStatus.APPROVED)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void alreadyReservedReservationIgnoresNewNotifications() { // idempotencia tras consolidar
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, NOW.plusSeconds(600))));
        when(paymentAttemptPort.registerIfAbsent(any(), any(), any(), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(command(PaymentStatus.APPROVED));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.IGNORED);
        verify(changeReservationStatus, never()).execute(any());
    }

    @Test
    void syntheticReferenceIsStableWhenModuleThreeOmitsIt() { // rechazo sin referencia externa
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.registerIfAbsent(eq(RESERVATION_ID),
                eq("m3-" + RESERVATION_ID + "-REJECTED"), eq("REJECTED"), any(), any())).thenReturn(true);

        ConfirmPaymentResult result = service.confirm(
                new ConfirmPaymentCommand(RESERVATION_ID, null, PaymentStatus.REJECTED, NOW, null));

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.PENDING_RETRY);
    }
}
