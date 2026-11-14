package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.payment;

import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentAttemptAdapter implements PaymentAttemptPort { // persistencia del historial de cobros

    private final PaymentAttemptJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public PaymentAttemptAdapter(PaymentAttemptJpaRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public boolean registerIfAbsent(UUID reservationId,
                                    String externalReference,
                                    String result,
                                    String detail,
                                    Instant consultedAt) { // devuelve false si la clave ya existia
        return jdbcTemplate.update("""
                INSERT INTO payment_attempt (reservation_id, external_reference, result, detail, consulted_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (reservation_id, external_reference, result) DO NOTHING
                """, reservationId, externalReference, result, detail, Timestamp.from(consultedAt)) == 1;
    }

    @Override
    public int countAttempts(UUID reservationId) {
        return (int) repository.countByReservationId(reservationId);
    }

    @Override
    public Optional<String> lastResult(UUID reservationId) {
        return jdbcTemplate.query("""
                SELECT result FROM payment_attempt WHERE reservation_id = ?
                ORDER BY consulted_at DESC, insertion_order DESC LIMIT 1
                """, rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(), reservationId);
    }

    @Override
    @Transactional
    public boolean claimLastRejectionForRetry(UUID reservationId) {
        return jdbcTemplate.update("""
                INSERT INTO payment_retry_claim (payment_attempt_id)
                SELECT id FROM (
                    SELECT id, result FROM payment_attempt WHERE reservation_id = ?
                    ORDER BY consulted_at DESC, insertion_order DESC LIMIT 1
                ) latest WHERE result = 'REJECTED'
                ON CONFLICT (payment_attempt_id) DO NOTHING
                """, reservationId) == 1;
    }
}
