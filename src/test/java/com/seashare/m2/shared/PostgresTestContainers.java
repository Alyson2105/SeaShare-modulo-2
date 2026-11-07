package com.seashare.m2.shared;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de pruebas reutilizable (T-21): un unico contenedor PostgreSQL 16
 * compartido por toda la JVM de pruebas.
 *
 * <p>Uso en un test: {@code @Import(PostgresTestContainers.class)} junto a
 * {@code @SpringBootTest}. Spring Boot inyecta la conexion del contenedor via
 * {@code @ServiceConnection}, con prioridad sobre el datasource de
 * {@code application.yaml}.</p>
 *
 * <p>Los tests existentes NO usan esta base: siguen contra el PostgreSQL de
 * {@code docker-compose.yml}. Ver informe de T-21 sobre el coste de migrarlos.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainers {

    // Contenedor unico compartido: arranca una vez y se reutiliza en la JVM de pruebas.
    // Ryuk lo detiene al terminar la corrida de Maven.
    private static final PostgreSQLContainer<?> SHARED_POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("seashare_m2_test");

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> sharedPostgresContainer() {
        if (!SHARED_POSTGRES.isRunning()) {
            SHARED_POSTGRES.start();
        }
        return SHARED_POSTGRES;
    }
}
