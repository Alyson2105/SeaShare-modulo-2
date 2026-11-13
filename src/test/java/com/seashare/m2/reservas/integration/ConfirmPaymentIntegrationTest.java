package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.ConfirmPaymentResult;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.LatePaymentReversalEvent;
import com.seashare.m2.reservas.application.dto.PaymentConfirmationOutcome;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.ConfirmPaymentUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.in.StartPaymentUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresTestContainers.class)
@Transactional
class ConfirmPaymentIntegrationTest { // CU-13: confirmacion de cobro con PostgreSQL real

    private static final Instant T0 = Instant.parse("2026-11-12T10:00:00Z");

    @Autowired
    private CreateReservationUseCase createReservationUseCase;

    @Autowired
    private StartPaymentUseCase startPaymentUseCase;

    @Autowired
    private ConfirmPaymentUseCase confirmPaymentUseCase;

    @Autowired
    private com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase changeReservationStatus;

    @Autowired
    private ReservationRepositoryPort reservationRepository;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoSpyBean
    private OutboxPort outboxPort;

    private UUID renterId;

    private UUID vesselId;

    @BeforeEach
    void setUp() {
        renterId = UUID.randomUUID();
        vesselId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(T0); // reloj controlado
    }

    private UUID createInitiated() { // reserva INITIATED con TTL de 15 minutos
        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                renterId,
                UUID.randomUUID().toString(),
                vesselId,
                LocalDate.of(2027, 8, 1),
                LocalDate.of(2027, 8, 3),
                2,
                "Juan Perez",
                "+573001234567",
                "juan@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("2500000.00")));
        return view.reservationId();
    }

    private void startPayment(UUID reservationId, String token) { // deja la reserva en PENDING_PAYMENT
        startPaymentUseCase.start(new StartPaymentCommand(reservationId, renterId, "RENTER", token));
    }

    private ConfirmPaymentResult confirm(UUID reservationId, PaymentStatus result, String reference) {
        return confirmPaymentUseCase.confirm(new ConfirmPaymentCommand(reservationId, reference, result, T0, null));
    }

    @Test
    void approvedWithinTtlReservesWithPaymentReference() { // FR-004
        UUID id = createInitiated();
        startPayment(id, "tok_1");
        clearInvocations(outboxPort);

        ConfirmPaymentResult result = confirm(id, PaymentStatus.APPROVED, "pg-8841");

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.CONFIRMED);
        Reservation saved = reservationRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.getPaymentReference()).isEqualTo("pg-8841"); // trazabilidad de la transaccion
        verify(outboxPort).enqueue(argThat((StatusChangedEvent event) ->
                event.reservationId().equals(id) && event.status() == ReservationStatus.RESERVED));
    }

    @Test
    void lateApprovalPublishesCancelledFlexibleOnceAndKeepsState() { // FR-006 y CU-13 plan L25
        UUID id = createInitiated();
        startPayment(id, "tok_1");
        clearInvocations(outboxPort);
        when(clockPort.now()).thenReturn(T0.plusSeconds(901)); // TTL vencido por un segundo

        ConfirmPaymentResult first = confirm(id, PaymentStatus.APPROVED, "pg-late");
        ConfirmPaymentResult second = confirm(id, PaymentStatus.APPROVED, "pg-late"); // ciclo repetido

        assertThat(first.outcome()).isEqualTo(PaymentConfirmationOutcome.LATE_REVERSAL);
        assertThat(second.outcome()).isEqualTo(PaymentConfirmationOutcome.DUPLICATE);
        assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING_PAYMENT); // el job de TTL la expira
        verify(outboxPort, times(1)).enqueue(any(LatePaymentReversalEvent.class)); // una sola reversion
    }

    @Test
    void rejectedChargeKeepsPendingPaymentAndAllowsRetry() { // FR-005 y CU-13
        UUID id = createInitiated();
        startPayment(id, "tok_1");
        clearInvocations(outboxPort);

        ConfirmPaymentResult rejected = confirm(id, PaymentStatus.REJECTED, "pg-2");

        assertThat(rejected.outcome()).isEqualTo(PaymentConfirmationOutcome.PENDING_RETRY);
        startPayment(id, "tok_2"); // reintento con otro token

        Reservation saved = reservationRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(outboxPort).enqueue(argThat((StatusChangedEvent event) ->
                event.reservationId().equals(id) && "tok_2".equals(event.paymentTokenRef())));
    }

    @Test
    void cancelledChargeFailsTheReservation() { // CANCELLED de M3 -> PAYMENT_FAILED
        UUID id = createInitiated();
        startPayment(id, "tok_1");

        ConfirmPaymentResult result = confirm(id, PaymentStatus.CANCELLED, "pg-3");

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.PAYMENT_FAILED);
        assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PAYMENT_FAILED);
    }

    @Test
    void duplicateApprovalDoesNotRepeatTransitions() { // FR-007
        UUID id = createInitiated();
        startPayment(id, "tok_1");
        clearInvocations(outboxPort);

        confirm(id, PaymentStatus.APPROVED, "pg-dup");
        ConfirmPaymentResult duplicate = confirm(id, PaymentStatus.APPROVED, "pg-dup");

        assertThat(duplicate.outcome()).isEqualTo(PaymentConfirmationOutcome.DUPLICATE);
        verify(outboxPort, times(1)).enqueue(argThat((StatusChangedEvent event) ->
                event.status() == ReservationStatus.RESERVED)); // una sola notificacion de reserva
    }

    @Test
    void approvedOnExpiredReservationPublishesReversal() { // cobro huerfano sobre una reserva ya expirada
        UUID id = createInitiated();
        startPayment(id, "tok_1");
        changeReservationStatus.execute(new com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand(
                id, ReservationStatus.EXPIRED, null,
                com.seashare.m2.reservas.domain.model.ActorType.SYSTEM, "TTL vencido"));
        clearInvocations(outboxPort);
        when(clockPort.now()).thenReturn(T0.plusSeconds(901));

        ConfirmPaymentResult result = confirm(id, PaymentStatus.APPROVED, "pg-orphan");

        assertThat(result.outcome()).isEqualTo(PaymentConfirmationOutcome.LATE_REVERSAL);
        assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.EXPIRED); // no cambia
        verify(outboxPort).enqueue(any(LatePaymentReversalEvent.class));
        verify(outboxPort, never()).enqueue(argThat((StatusChangedEvent event) ->
                event.status() == ReservationStatus.RESERVED));
    }
}
