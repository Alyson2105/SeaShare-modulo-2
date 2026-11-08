package com.seashare.m2.shared;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;

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
 */
@TestConfiguration(proxyBeanMethods = false)
public class RabbitTestContainers { // contenedor rabbitmq unico compartido para la suite

    // Contenedor unico compartido: arranca una vez y se reutiliza en la JVM de pruebas.
    private static final RabbitMQContainer SHARED_RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @Bean
    @ServiceConnection
    public RabbitMQContainer sharedRabbitContainer() {
        if (!SHARED_RABBIT.isRunning()) {
            SHARED_RABBIT.start();
        }
        return SHARED_RABBIT;
    }
}
