package com.seashare.m2.disputa.infrastructure.adapter.out.messaging.config;

import com.seashare.m2.disputa.infrastructure.adapter.out.messaging.DisputeMessagingProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ propia del contexto disputa (T-18c), alineada con Modulo 3.
 *
 * <p>Declara la cola de M3 {@code finance.guarantee-dispute.v1} como quorum con
 * dead-lettering al exchange por defecto, su DLQ y el binding
 * {@code reservation.dispute.updated}. No se declara un exchange DLX propio.</p>
 */
@Configuration
public class DisputeRabbitTopologyConfig {

    @Bean
    public Declarables disputeTopology(DisputeMessagingProperties properties) { // declara cola, DLQ y bindings
        TopicExchange exchange = new TopicExchange(properties.getExchange(), true, false); // exchange topic durable

        Queue disputeQueue = QueueBuilder.durable(properties.getQueueDispute())
                .quorum() // mismo tipo que M3
                .deadLetterExchange("") // exchange por defecto
                .deadLetterRoutingKey(properties.dlqDispute()) // rk hacia su DLQ
                .build();

        Queue disputeDlq = QueueBuilder.durable(properties.dlqDispute()).build(); // DLQ de la cola de disputa

        Binding disputeBinding = BindingBuilder.bind(disputeQueue)
                .to(exchange)
                .with(properties.getRoutingKeyDisputeUpdated()); // reservation.dispute.updated

        return new Declarables(exchange, disputeQueue, disputeDlq, disputeBinding);
    }
}
