package com.seashare.m2.shared;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.util.List;

/**
 * Base de pruebas reutilizable (T-02): un unico contenedor RabbitMQ
 * {@code rabbitmq:3-management} compartido por toda la JVM de pruebas.
 *
 * <p>Uso en un test: {@code @Import(RabbitTestContainers.class)} junto a
 * {@code @SpringBootTest}. Spring Boot inyecta la conexion AMQP del contenedor
 * via {@code @ServiceConnection} (RabbitContainerConnectionDetailsFactory),
 * con prioridad sobre el host/puerto de application.yaml.</p>
 *
 * <p>Patron identico a {@link PostgresTestContainers}: el contenedor arranca
 * una vez y Ryuk lo detiene al terminar la corrida de Maven.</p>
 *
 * <p>En equipos donde Windows reserva rangos efimeros de puertos, se pueden fijar
 * los puertos publicados con {@code SEASHARE_TEST_RABBIT_PORT} (AMQP) y
 * {@code SEASHARE_TEST_RABBIT_MANAGEMENT_PORT} (consola). En ese caso solo se
 * publican esos dos puertos.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class RabbitTestContainers { // contenedor rabbitmq unico compartido para la suite

    private static final String FIXED_AMQP_ENV = "SEASHARE_TEST_RABBIT_PORT"; // puerto amqp opcional

    private static final String FIXED_MANAGEMENT_ENV = "SEASHARE_TEST_RABBIT_MANAGEMENT_PORT"; // puerto ui opcional

    private static final int AMQP_PORT = 5672; // puerto amqp dentro del contenedor

    private static final int MANAGEMENT_PORT = 15672; // consola de administracion

    // Contenedor unico compartido: arranca una vez y se reutiliza en la JVM de pruebas.
    // Ryuk lo detiene al terminar la corrida de Maven.
    private static final RabbitMQContainer SHARED_RABBIT = buildContainer();

    private static RabbitMQContainer buildContainer() { // configura el contenedor compartido
        RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3-management");
        // Arranque lento en maquinas con pocos recursos: espera explicita y amplia.
        container.waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1)
                .withStartupTimeout(Duration.ofMinutes(3)));

        int hostAmqp = readPort(FIXED_AMQP_ENV, AMQP_PORT); // puerto amqp publicado
        int hostManagement = readPort(FIXED_MANAGEMENT_ENV, MANAGEMENT_PORT); // puerto ui publicado
        String requestedAmqp = System.getenv(FIXED_AMQP_ENV); // solo se fija si el entorno lo pide

        if (requestedAmqp != null && !requestedAmqp.isBlank()) { // evita rangos reservados por el sistema operativo
            container.setExposedPorts(List.of(AMQP_PORT, MANAGEMENT_PORT)); // publica solo estos dos
            container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(hostAmqp), ExposedPort.tcp(AMQP_PORT)),
                    new PortBinding(Ports.Binding.bindPort(hostManagement), ExposedPort.tcp(MANAGEMENT_PORT))));
        }
        return container; // contenedor listo para arrancar
    }

    private static int readPort(String envName, int fallback) { // lee un puerto opcional del entorno
        String value = System.getenv(envName);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    @Bean
    @ServiceConnection
    public RabbitMQContainer sharedRabbitContainer() {
        if (!SHARED_RABBIT.isRunning()) {
            SHARED_RABBIT.start();
        }
        return SHARED_RABBIT;
    }
}
