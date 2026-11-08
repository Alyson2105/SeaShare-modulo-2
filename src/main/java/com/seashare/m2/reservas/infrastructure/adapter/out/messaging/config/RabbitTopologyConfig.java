package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ del modulo 2 (T-02).
 *
 * <p>Declaraciones:
 * exchange topic durable {@code seashare.reservations}, exchange directo de
 * dead-lettering, dos colas de M3 con sus DLQ y los bindings por routing key
 * ({@code reservation.status.changed} y {@code reservation.info.provided}).
 * Spring Boot las declara al abrir la primera conexion con el broker
 * (RabbitAdmin, {@code spring.rabbitmq.dynamic=true}).</p>
 *
 * <p>Los nombres viven en application.yaml ({@code m2.messaging.*}) via
 * {@link MessagingProperties}; la cola de disputa (CU-18) queda para T-18c.</p>
 */
@Configuration
public class RabbitTopologyConfig { // configuracion de declaraciones de la topologia rabbitmq

    private static final String DLX_ARG = "x-dead-letter-exchange"; // argumento de cola para dead-lettering

    private static final String DLQ_ROUTING_ARG = "x-dead-letter-routing-key"; // routing key hacia la DLQ

    @Bean
    public Declarables seashareReservationsTopology(MessagingProperties properties) { // declara topologia completa
        TopicExchange exchange = new TopicExchange(properties.getExchange(), true, false); // exchange topic durable
        DirectExchange deadLetterExchange = // exchange directo de dead-lettering
                new DirectExchange(properties.getDeadLetterExchange(), true, false); // durable y no auto-delete

        Queue statusQueue = QueueBuilder.durable(properties.getQueueStatus()) // cola de cambios de estado
                .withArgument(DLX_ARG, properties.getDeadLetterExchange()) // deriva a DLX si se rechaza
                .withArgument(DLQ_ROUTING_ARG, properties.getQueueStatus()) // rk hacia su DLQ
                .build(); // construye cola durable

        Queue infoQueue = QueueBuilder.durable(properties.getQueueInfo()) // cola de informacion de reserva
                .withArgument(DLX_ARG, properties.getDeadLetterExchange()) // deriva a DLX si se rechaza
                .withArgument(DLQ_ROUTING_ARG, properties.getQueueInfo()) // rk hacia su DLQ
                .build(); // construye cola durable

        Queue statusDlq = QueueBuilder.durable(properties.dlqStatus()).build(); // DLQ de la cola de estado
        Queue infoDlq = QueueBuilder.durable(properties.dlqInfo()).build(); // DLQ de la cola de informacion

        Binding statusBinding = BindingBuilder.bind(statusQueue) // binding cola de estado
                .to(exchange) // hacia el exchange topic
                .with(properties.getRoutingKeyStatusChanged()); // routing key reservation.status.changed

        Binding infoBinding = BindingBuilder.bind(infoQueue) // binding cola de informacion
                .to(exchange) // hacia el exchange topic
                .with(properties.getRoutingKeyInfoProvided()); // routing key reservation.info.provided

        Binding statusDlqBinding = BindingBuilder.bind(statusDlq) // binding DLQ de estado
                .to(deadLetterExchange) // hacia el DLX
                .with(properties.getQueueStatus()); // rk igual al nombre de la cola origen

        Binding infoDlqBinding = BindingBuilder.bind(infoDlq) // binding DLQ de informacion
                .to(deadLetterExchange) // hacia el DLX
                .with(properties.getQueueInfo()); // rk igual al nombre de la cola origen

        return new Declarables( // grupo de declaraciones que RabbitAdmin aplica juntas
                exchange, deadLetterExchange, // exchanges
                statusQueue, infoQueue, statusDlq, infoDlq, // colas y DLQs
                statusBinding, infoBinding, statusDlqBinding, infoDlqBinding); // bindings
    }
}
