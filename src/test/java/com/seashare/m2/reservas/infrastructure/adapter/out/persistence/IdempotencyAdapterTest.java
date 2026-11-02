package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.out.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.service.CreateReservationService;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class IdempotencyAdapterTest {

    private static final String TEST_ENDPOINT = "CREATE_RESERVATION_TEST";

    @Autowired
    private IdempotencyAdapter idempotencyAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreateReservationService createReservationService;

    @MockitoBean
    private FleetInfoPort fleetInfoPort;

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort;

    @MockitoBean
    private ClockPort clockPort;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reservation_status_audit");
        jdbcTemplate.update("DELETE FROM outbox_message");
        jdbcTemplate.update("DELETE FROM idempotency_key");
        jdbcTemplate.update("DELETE FROM reservation");
    }

    @Test
    void secondClaimWithinWindowReturnsFalseAndPreservesOriginal() {
        UUID key = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        when(clockPort.now()).thenReturn(now);
        FleetInfo info = new FleetInfo(UUID.randomUUID(), 10, "America/Bogota");
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(info);
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);

        CreateReservationCommand command = new CreateReservationCommand(
                UUID.randomUUID(), key.toString(), UUID.randomUUID(),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                2, "Ana", "3000000000", "ana@test.com", LocalTime.of(9, 0), new BigDecimal("100.0000")
        );
        ReservationView first = createReservationService.execute(command);
        ReservationView second = createReservationService.execute(command);
        assertThat(second.reservationId()).isEqualTo(first.reservationId());
    }

    @Test
    void claimAfterWindowExpiresReturnsTrue() {
        String key = UUID.randomUUID().toString();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        boolean first = idempotencyAdapter.tryClaim(key, TEST_ENDPOINT, now, Duration.ofSeconds(60));
        assertThat(first).isTrue();
        boolean second = idempotencyAdapter.tryClaim(key, TEST_ENDPOINT, now.plusSeconds(61), Duration.ofSeconds(60));
        assertThat(second).isTrue();
    }

    @Test
    void concurrentClaimsOnlyOneWins() throws Exception {
        String key = UUID.randomUUID().toString();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger wins = new AtomicInteger(0);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (idempotencyAdapter.tryClaim(key, TEST_ENDPOINT, now, Duration.ofSeconds(60))) {
                        wins.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(wins.get()).isEqualTo(1);
    }

    @Test
    void completeWithoutClaimThrowsIllegalStateException() {
        String key = UUID.randomUUID().toString();
        assertThatThrownBy(() -> idempotencyAdapter.complete(key, TEST_ENDPOINT, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }
}
