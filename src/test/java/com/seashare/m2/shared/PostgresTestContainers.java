package com.seashare.m2.shared;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
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
 * <p>Todos los tests de integracion con contexto Spring usan esta base
 * (migracion c6e8ff2); ninguno depende del PostgreSQL de docker-compose.
 * Ver informe de T-21 sobre el coste de migrarlos.</p>
 *
 * <p>En equipos donde Windows reserva rangos efimeros de puertos, se puede fijar
 * el puerto publicado con la variable {@code SEASHARE_TEST_POSTGRES_PORT}; sin
 * ella se mantiene el puerto dinamico habitual.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainers {

    private static final String FIXED_PORT_ENV = "SEASHARE_TEST_POSTGRES_PORT"; // puerto publicado opcional

    // Contenedor unico compartido: arranca una vez y se reutiliza en la JVM de pruebas.
    // Ryuk lo detiene al terminar la corrida de Maven.
    private static final PostgreSQLContainer<?> SHARED_POSTGRES = buildContainer();

    private static PostgreSQLContainer<?> buildContainer() { // configura el contenedor compartido
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("seashare_m2_test");
        String fixedPort = System.getenv(FIXED_PORT_ENV); // puerto fijo solo si el entorno lo pide
        if (fixedPort != null && !fixedPort.isBlank()) { // evita rangos reservados por el sistema operativo
            int hostPort = Integer.parseInt(fixedPort.trim()); // puerto publicado en el host
            container.setExposedPorts(java.util.List.of(5432)); // solo se publica el puerto de PostgreSQL
            container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(hostPort), ExposedPort.tcp(5432)))); // mapea 5432
        }
        return container; // contenedor listo para arrancar
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> sharedPostgresContainer() {
        if (!SHARED_POSTGRES.isRunning()) {
            SHARED_POSTGRES.start();
        }
        return SHARED_POSTGRES;
    }
}
