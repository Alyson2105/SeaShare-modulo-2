package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CalculatedValue;
import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.dto.StartPaymentResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class StartPaymentServiceTest { // CU-03: validaciones previas a la transaccion

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private static final UUID RESERVATION_ID = UUID.randomUUID();

    private static final UUID RENTER_ID = UUID.randomUUID();

    private static final UUID VESSEL_ID = UUID.randomUUID();

    @Mock
    private ReservationRepositoryPort reservationRepository;

    @Mock
    private CalculatedValuePort calculatedValuePort;

    @Mock
    private FleetOperationalStatusPort fleetOperationalStatusPort;

    @Mock
    private PaymentAttemptPort paymentAttemptPort;

    @Mock
    private StartPaymentWriter writer;

    @Mock
    private ClockPort clockPort;

    private StartPaymentService service;

    @BeforeEach
    void setUp() {
        service = new StartPaymentService(reservationRepository, calculatedValuePort,
                fleetOperationalStatusPort, paymentAttemptPort, writer, clockPort);
    }

    private Reservation reservation(ReservationStatus status, Instant ttl) {
        return Reservation.builder()
                .id(RESERVATION_ID)
                .renterId(RENTER_ID)
                .ownerId(UUID.randomUUID())
                .vesselId(VESSEL_ID)
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

    private StartPaymentCommand command(String role) {
        return new StartPaymentCommand(RESERVATION_ID, RENTER_ID, role, "tok_123");
    }

    @Test
    void nonRenterCannotStartPayment() { // solo el arrendatario paga
        assertThatThrownBy(() -> service.start(command("OWNER")))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(reservationRepository, never()).findById(any());
    }

    @Test
    void unknownReservationIs404() { // reserva inexistente
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(command("RENTER")))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void otherRenterIsForbidden() { // FR-001: solo el titular
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.INITIATED, NOW.plusSeconds(600))));
        StartPaymentCommand foreign = new StartPaymentCommand(RESERVATION_ID, UUID.randomUUID(), "RENTER", "tok");

        assertThatThrownBy(() -> service.start(foreign))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.RESERVATION_NOT_OWNED.getCode()));
    }

    @Test
    void nonInitiatedReservationIsConflict() { // FR-002
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED, NOW.plusSeconds(600))));

        assertThatThrownBy(() -> service.start(command("RENTER")))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATE.getCode()));
    }

    @Test
    void expiredTtlIsConflict() { // corte exacto del TTL
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.INITIATED, NOW)));

        assertThatThrownBy(() -> service.start(command("RENTER")))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.RESERVATION_EXPIRED.getCode()));
    }

    @Test
    void calculationFailureAbortsWithoutTransition() { // FR-003: M3 caido
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.INITIATED, NOW.plusSeconds(600))));
        when(calculatedValuePort.getCalculatedValue(RESERVATION_ID)).thenThrow(
                new ReservationRuleException(ErrorCode.CALCULATION_UNAVAILABLE, "M3 caido"));

        assertThatThrownBy(() -> service.start(command("RENTER")))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode()));
        verify(writer, never()).startPayment(any(), any(), any(), any()); // no se transiciona
    }

    @Test
    void unavailableVesselIsConflict() { // FR-005: M1 ya la bloqueo
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.INITIATED, NOW.plusSeconds(600))));
        when(calculatedValuePort.getCalculatedValue(RESERVATION_ID))
                .thenReturn(new CalculatedValue(new BigDecimal("1"), new BigDecimal("1"),
                        new BigDecimal("1"), new BigDecimal("3")));
        when(fleetOperationalStatusPort.isAvailable(VESSEL_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.start(command("RENTER")))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_AVAILABLE.getCode()));
        verify(writer, never()).startPayment(any(), any(), any(), any());
    }

    @Test
    void validPaymentDelegatesToTransactionalWriter() { // camino feliz
        CalculatedValue value = new CalculatedValue(new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("20"), new BigDecimal("130"));
        StartPaymentResult expected = new StartPaymentResult(RESERVATION_ID, ReservationStatus.PENDING_PAYMENT,
                VESSEL_ID, value, 600, null, 1);
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.INITIATED, NOW.plusSeconds(600))));
        when(calculatedValuePort.getCalculatedValue(RESERVATION_ID)).thenReturn(value);
        when(fleetOperationalStatusPort.isAvailable(VESSEL_ID)).thenReturn(true);
        when(writer.startPayment(eq(RESERVATION_ID), eq(value), eq("tok_123"), eq(NOW))).thenReturn(expected);

        StartPaymentResult result = service.start(command("RENTER"));

        assertThat(result.status()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(writer).startPayment(RESERVATION_ID, value, "tok_123", NOW); // token hacia el evento PENDING
    }

    @Test
    void retryAfterRejectedReusesThePersistedCalculation() { // CU-13: reintento con otro token
        CalculatedValue persisted = new CalculatedValue(new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("20"), new BigDecimal("130"));
        StartPaymentResult expected = new StartPaymentResult(RESERVATION_ID, ReservationStatus.PENDING_PAYMENT,
                VESSEL_ID, persisted, 600, null, 2);
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.lastResult(RESERVATION_ID)).thenReturn(Optional.of("REJECTED"));
        when(writer.startPayment(eq(RESERVATION_ID), eq(null), eq("tok_123"), eq(NOW))).thenReturn(expected);

        StartPaymentResult result = service.start(command("RENTER"));

        assertThat(result.attempt()).isEqualTo(2);
        verify(calculatedValuePort, never()).getCalculatedValue(any()); // no se vuelve a pedir el calculo
        verify(fleetOperationalStatusPort, never()).isAvailable(any()); // el activo ya estaba retenido
        verify(writer).startPayment(RESERVATION_ID, null, "tok_123", NOW);
    }

    @Test
    void retryWithoutRejectionIsConflict() { // ya hay un cobro en proceso
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservation(ReservationStatus.PENDING_PAYMENT, NOW.plusSeconds(600))));
        when(paymentAttemptPort.lastResult(RESERVATION_ID)).thenReturn(Optional.of("APPROVED"));

        assertThatThrownBy(() -> service.start(command("RENTER")))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.RETRY_NOT_ALLOWED.getCode()));
        verify(writer, never()).startPayment(any(), any(), any(), any());
    }
}
