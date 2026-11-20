package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ del modulo 2 (T-02), alineada con Modulo 3.
 *
 * <p>Las colas compartidas se declaran como quorum con dead-lettering al exchange
 * por defecto y routing key {@code <cola>.dlq}, igual que M3. No se declara un
 * exchange DLX propio: M3 es la referencia de la topologia.</p>
 */
@Configuration
public class RabbitTopologyConfig { // configuracion de declaraciones de la topologia rabbitmq

    @Bean
    public Declarables seashareReservationsTopology(MessagingProperties properties) { // declara topologia completa
        TopicExchange exchange = new TopicExchange(properties.getExchange(), true, false); // exchange topic durable

        Queue statusQueue = QueueBuilder.durable(properties.getQueueStatus()) // cola de cambios de estado
                .quorum() // mismo tipo que M3
                .deadLetterExchange("") // exchange por defecto
                .deadLetterRoutingKey(properties.dlqStatus()) // rk hacia su DLQ
                .build(); // construye cola quorum

        Queue infoQueue = QueueBuilder.durable(properties.getQueueInfo()) // cola de informacion de reserva
                .quorum() // mismo tipo que M3
                .deadLetterExchange("") // exchange por defecto
                .deadLetterRoutingKey(properties.dlqInfo()) // rk hacia su DLQ
                .build(); // construye cola quorum

        Queue statusDlq = QueueBuilder.durable(properties.dlqStatus()).build(); // DLQ de la cola de estado
        Queue infoDlq = QueueBuilder.durable(properties.dlqInfo()).build(); // DLQ de la cola de informacion

        Binding statusBinding = BindingBuilder.bind(statusQueue) // binding cola de estado
                .to(exchange) // hacia el exchange topic
                .with(properties.getRoutingKeyStatusChanged()); // routing key reservation.status.changed

        Binding infoBinding = BindingBuilder.bind(infoQueue) // binding cola de informacion
                .to(exchange) // hacia el exchange topic
                .with(properties.getRoutingKeyInfoProvided()); // routing key reservation.info.provided

        return new Declarables( // grupo de declaraciones que RabbitAdmin aplica juntas
                exchange, // exchange topic
                statusQueue, infoQueue, statusDlq, infoDlq, // colas y DLQs
                statusBinding, infoBinding); // bindings
    }
}
