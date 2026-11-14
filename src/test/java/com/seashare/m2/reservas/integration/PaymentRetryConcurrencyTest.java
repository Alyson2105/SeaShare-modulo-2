package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ConfirmPaymentCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.StartPaymentCommand;
import com.seashare.m2.reservas.application.port.in.ConfirmPaymentUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.in.StartPaymentUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(PostgresTestContainers.class)
class PaymentRetryConcurrencyTest {
    @Autowired CreateReservationUseCase create;
    @Autowired StartPaymentUseCase start;
    @Autowired ConfirmPaymentUseCase confirm;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean FleetInfoPort fleet;
    @MockitoBean FleetOperationalStatusPort operational;

    @Test
    void simultaneousPaymentsForOverlappingReservationsHaveOneWinner() throws Exception {
        UUID renter = UUID.randomUUID();
        UUID vessel = UUID.randomUUID();
        when(fleet.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(operational.isAvailable(any())).thenReturn(true);
        var ids = new java.util.ArrayList<UUID>();
        for (int i = 0; i < 2; i++) {
            ids.add(create.execute(new CreateReservationCommand(renter, UUID.randomUUID().toString(), vessel,
                    LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 3), 2, "Juan Perez", "+573001234567",
                    "juan@example.com", LocalTime.of(9, 0), new BigDecimal("2500000.00"))).reservationId());
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = ids.stream().map(id -> pool.submit(() -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    start.start(new StartPaymentCommand(id, renter, "RENTER", "simulated-token"));
                    return "accepted";
                } catch (ReservationRuleException e) {
                    return e.getCode();
                }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(java.util.List.of(tasks.get(0).get(20, TimeUnit.SECONDS), tasks.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("accepted", "CONCURRENCY_CONFLICT");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation WHERE vessel_id = ? AND status = 'PENDING_PAYMENT'",
                Integer.class, vessel)).isEqualTo(1);
    }

    @Test
    void concurrentRetriesConsumeOneRejectionOnlyOnce() throws Exception {
        UUID renter = UUID.randomUUID();
        UUID vessel = UUID.randomUUID();
        when(fleet.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(operational.isAvailable(any())).thenReturn(true);
        UUID id = create.execute(new CreateReservationCommand(renter, UUID.randomUUID().toString(), vessel,
                LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 3), 2, "Juan Perez", "+573001234567",
                "juan@example.com", LocalTime.of(9, 0), new BigDecimal("2500000.00"))).reservationId();
        start.start(new StartPaymentCommand(id, renter, "RENTER", "first-token"));
        confirm.confirm(new ConfirmPaymentCommand(id, "simulated-rejection", PaymentStatus.REJECTED, Instant.now(), null));
        int before = jdbc.queryForObject("SELECT count(*) FROM outbox_message WHERE payload LIKE ?",
                Integer.class, "%" + id + "%");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(i -> pool.submit(() -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    start.start(new StartPaymentCommand(id, renter, "RENTER", "retry-token-" + i));
                    return "accepted";
                } catch (ReservationRuleException e) {
                    return e.getCode();
                }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(java.util.List.of(tasks.get(0).get(20, TimeUnit.SECONDS), tasks.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("accepted", "RETRY_NOT_ALLOWED");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_message WHERE payload LIKE ?",
                Integer.class, "%" + id + "%")).isEqualTo(before + 1);
        confirm.confirm(new ConfirmPaymentCommand(id, "another-simulated-rejection", PaymentStatus.REJECTED,
                Instant.now(), null));
        start.start(new StartPaymentCommand(id, renter, "RENTER", "next-retry-token"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_message WHERE payload LIKE ?",
                Integer.class, "%" + id + "%")).isEqualTo(before + 2);
    }
}
