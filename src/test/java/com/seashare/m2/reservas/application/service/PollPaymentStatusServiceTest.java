package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.PaymentStatusResult;
import com.seashare.m2.reservas.application.port.in.ConfirmPaymentUseCase;
import com.seashare.m2.reservas.application.port.out.PaymentStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollPaymentStatusServiceTest { // CU-13 canal B: ciclo de consulta

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private static final UUID FIRST = UUID.randomUUID();

    private static final UUID SECOND = UUID.randomUUID();

    @Mock
    private ReservationRepositoryPort reservationRepository;

    @Mock
    private PaymentStatusPort paymentStatusPort;

    @Mock
    private ConfirmPaymentUseCase confirmPayment;

    @Mock
    private ClockPort clockPort;

    private PollPaymentStatusService service;

    @BeforeEach
    void setUp() {
        service = new PollPaymentStatusService(reservationRepository, paymentStatusPort,
                confirmPayment, clockPort, 50, 30);
    }

    @Test
    void noCandidatesDoesNotQueryModuleThree() { // sin reservas no hay llamadas
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findPaymentCandidates(any(), anyInt())).thenReturn(List.of());

        assertThat(service.execute()).isZero();
        verify(paymentStatusPort, never()).getStatus(any());
    }

    @Test
    void inProcessIsSkipped() { // sin resultado definitivo no se invoca la logica
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findPaymentCandidates(any(), anyInt())).thenReturn(List.of(FIRST));
        when(paymentStatusPort.getStatus(FIRST))
                .thenReturn(new PaymentStatusResult(PaymentStatus.IN_PROCESS, null, null));

        assertThat(service.execute()).isZero();
        verify(confirmPayment, never()).confirm(any());
    }

    @Test
    void approvedResultIsDeliveredToTheConfirmationLogic() { // ambos canales comparten la logica
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findPaymentCandidates(any(), anyInt())).thenReturn(List.of(FIRST));
        when(paymentStatusPort.getStatus(FIRST))
                .thenReturn(new PaymentStatusResult(PaymentStatus.APPROVED, null, "pg-1"));

        assertThat(service.execute()).isEqualTo(1);
        verify(confirmPayment).confirm(any(ConfirmPaymentCommand.class));
    }

    @Test
    void oneFailureDoesNotStopTheCycle() { // aislamiento por reserva
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findPaymentCandidates(any(), anyInt())).thenReturn(List.of(FIRST, SECOND));
        when(paymentStatusPort.getStatus(FIRST)).thenThrow(new IllegalStateException("boom"));
        when(paymentStatusPort.getStatus(SECOND))
                .thenReturn(new PaymentStatusResult(PaymentStatus.REJECTED, "declined", "pg-2"));

        assertThat(service.execute()).isEqualTo(1); // la segunda si se procesa
        verify(confirmPayment).confirm(any(ConfirmPaymentCommand.class));
    }

    @Test
    void candidatesUseTheRecentWindow() { // incluye cierres recientes para cobros huerfanos
        when(clockPort.now()).thenReturn(NOW);
        when(reservationRepository.findPaymentCandidates(eq(NOW.minusSeconds(1800)), eq(50)))
                .thenReturn(List.of());

        service.execute();

        verify(reservationRepository).findPaymentCandidates(NOW.minusSeconds(1800), 50);
    }
}
