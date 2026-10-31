package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyAdapter implements IdempotencyPort { // adaptador de infraestructura para control de idempotencia con jdbc

    private final JdbcTemplate jdbcTemplate; // cliente jdbc directo para atomicidad sin jpa

    public IdempotencyAdapter(JdbcTemplate jdbcTemplate) { // inyeccion del cliente jdbc
        this.jdbcTemplate = jdbcTemplate; // asigna jdbcTemplate
    }

    // [NEEDS CLARIFICATION] el plan no define que pasa con la misma clave y distinto cuerpo: se devuelve la reserva original sin comparar el cuerpo;
    // [NEEDS CLARIFICATION] pasados 60 s la clave expira y se reutiliza la fila (la llave primaria es key+endpoint).
    @Override
    public boolean tryClaim(String key, String endpoint, Instant now, Duration window) { // reclama clave de forma atomica con upsert condicional
        String sql = "INSERT INTO idempotency_key (key, endpoint, response_ref, created_at) " + // sentencia de insercion inicial
                "VALUES (?, ?, NULL, ?) " + // valores con respuesta pendiente
                "ON CONFLICT (key, endpoint) DO UPDATE " + // clausula de conflicto por clave primaria
                "SET response_ref = NULL, created_at = EXCLUDED.created_at " + // reinicia referencia si ya vencio
                "WHERE idempotency_key.created_at <= ?"; // condicion de expiracion de ventana
        OffsetDateTime nowUtc = now.atOffset(ZoneOffset.UTC); // marca temporal actual en utc
        OffsetDateTime thresholdUtc = now.minus(window).atOffset(ZoneOffset.UTC); // umbral temporal limite en utc
        int rowsAffected = jdbcTemplate.update(sql, key, endpoint, nowUtc, thresholdUtc); // ejecuta upsert atomico
        return rowsAffected == 1; // true si inserto o actualizo por expiracion
    }

    @Override
    public void complete(String key, String endpoint, UUID reservationId) { // asocia la reserva a la clave completada
        String sql = "UPDATE idempotency_key SET response_ref = ? WHERE key = ? AND endpoint = ?"; // actualiza referencia del resultado
        jdbcTemplate.update(sql, reservationId.toString(), key, endpoint); // persiste id de reserva completada
    }

    @Override
    public Optional<UUID> findReservationId(String key, String endpoint) { // consulta referencia de reserva existente
        String sql = "SELECT response_ref FROM idempotency_key WHERE key = ? AND endpoint = ?"; // consulta clave por endpoint
        List<String> results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("response_ref"), key, endpoint); // mapea respuesta
        if (results.isEmpty() || results.get(0) == null || results.get(0).isBlank()) { // valida presencia y no vacuidad
            return Optional.empty(); // retorna vacio si no hay fila o es nulo
        }
        try {
            return Optional.of(UUID.fromString(results.get(0).trim())); // transforma texto a uuid
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // retorna vacio si formato es invalido
        }
    }
}
