package com.seashare.m2.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de humo (T-21): la aplicacion conecta a un PostgreSQL levantado con
 * Testcontainers y Flyway aplica las migraciones. Un solo test: la
 * infraestructura queda lista para futuras tareas (T-14, T-15).
 */
@SpringBootTest
@Import(PostgresTestContainers.class)
class PostgresSmokeTest {

    @Autowired
    private PostgreSQLContainer<?> postgres;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appConnectsAndFlywayMigrationsAreApplied() {
        assertTrue(postgres.isRunning(), "el contenedor PostgreSQL debe estar levantado");

        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);

        assertEquals(0, failed, "ninguna migracion debe haber fallado");
        assertTrue(applied >= 3, "Flyway debe haber aplicado V1, V2 y V3 (aplicadas: " + applied + ")");

        // ddl-auto=validate ya comprobo las entidades al arrancar el contexto;
        // esta consulta demuestra que se puede leer una tabla real del esquema
        Integer reservations = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM reservation", Integer.class);
        assertTrue(reservations >= 0, "la tabla reservation debe existir y ser consultable");
    }
}
