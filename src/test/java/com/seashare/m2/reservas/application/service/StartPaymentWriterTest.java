package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StartPaymentWriterTest {
    @Test
    void ttlIsRecheckedAfterWaitingForTheVesselLock() {
        var repository = mock(ReservationRepositoryPort.class);
        var changes = mock(ChangeReservationStatusUseCase.class);
        var attempts = mock(PaymentAttemptPort.class);
        var outbox = mock(OutboxPort.class);
        var clock = mock(ClockPort.class);
        var writer = new StartPaymentWriter(repository, changes, attempts, outbox, clock);
        UUID id = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-11-13T10:00:00Z");
        var reservation = Reservation.builder().id(id).vesselId(UUID.randomUUID())
                .status(ReservationStatus.INITIATED).ttlExpiresAt(expiresAt).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(reservation));
        when(clock.now()).thenReturn(expiresAt);

        assertThatThrownBy(() -> writer.startPayment(id, null, "token", expiresAt.minusSeconds(1)))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getCode())
                                .isEqualTo(ErrorCode.RESERVATION_EXPIRED.getCode()));
        verifyNoInteractions(changes, attempts, outbox);
    }
}
