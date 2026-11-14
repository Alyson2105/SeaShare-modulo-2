package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.CalculationAuditEntry;
import com.seashare.m2.reservas.application.port.out.CalculationAuditPort;
import com.seashare.m2.shared.PostgresTestContainers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestContainers.class)
@Transactional
class CalculationAuditIntegrationTest { // CU-12 FR-010: el asiento de calculo se persiste con montos y estado

    @Autowired
    private CalculationAuditPort calculationAuditPort; // puerto real bajo prueba

    @Autowired
    private JdbcTemplate jdbcTemplate; // lectura directa para verificar la fila

    @Autowired
    private EntityManager entityManager; // sincroniza JPA antes de consultar por JDBC

    @Test
    void successfulCalculationIsPersistedWithAllAmounts() { // el asiento guarda reserva, montos, estado y hora
        UUID reservationId = UUID.randomUUID(); // reserva simulada local
        Instant occurredAt = Instant.parse("2026-11-13T10:15:30Z"); // instante deterministico

        calculationAuditPort.record(new CalculationAuditEntry(reservationId, null,
                new BigDecimal("9600000.00"), new BigDecimal("120000.00"),
                new BigDecimal("320000.00"), new BigDecimal("10040000.00"), "SUCCESS", occurredAt));
        entityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM calculation_audit WHERE reservation_id = ?", reservationId);
        assertThat(row.get("calculation_status")).isEqualTo("SUCCESS");
        assertThat((BigDecimal) row.get("total_amount")).isEqualByComparingTo("10040000.00");
        assertThat((BigDecimal) row.get("rental_amount")).isEqualByComparingTo("9600000.00");
        assertThat(row.get("operation_id")).isNull(); // el contrato externo no entrega id de operacion
        assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isEqualTo(occurredAt);
    }

    @Test
    void failedCalculationIsPersistedWithoutAmounts() { // un fallo tambien deja rastro auditable
        UUID reservationId = UUID.randomUUID(); // reserva simulada local

        calculationAuditPort.record(new CalculationAuditEntry(reservationId, null,
                null, null, null, null, "FAILED", Instant.parse("2026-11-13T10:16:00Z")));
        entityManager.flush();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM calculation_audit WHERE reservation_id = ?", reservationId);
        assertThat(row.get("calculation_status")).isEqualTo("FAILED");
        assertThat(row.get("total_amount")).isNull();
        assertThat(row.get("rental_amount")).isNull();
    }
}
