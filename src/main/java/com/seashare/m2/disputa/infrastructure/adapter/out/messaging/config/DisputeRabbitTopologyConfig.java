package com.seashare.m2.disputa.infrastructure.adapter.out.messaging.config;

import com.seashare.m2.disputa.infrastructure.adapter.out.messaging.DisputeMessagingProperties;
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
 * Topologia RabbitMQ propia del contexto disputa (T-18c).
 *
 * <p>Declara la cola de M3 {@code finance.guarantee-dispute.v1}, su DLQ y el
 * binding {@code reservation.dispute.updated}, reutilizando el exchange topic
 * compartido. Se mantiene separada de la topologia de reservas para no tocar
 * archivos de ese contexto; redeclarar el mismo exchange con argumentos
 * identicos es una operacion idempotente en RabbitMQ.</p>
 */
@Configuration
public class DisputeRabbitTopologyConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange"; // argumento de cola para dead-lettering

    private static final String DLQ_ROUTING_ARG = "x-dead-letter-routing-key"; // routing key hacia la DLQ

    @Bean
    public Declarables disputeTopology(DisputeMessagingProperties properties) { // declara cola, DLQ y bindings
        TopicExchange exchange = new TopicExchange(properties.getExchange(), true, false); // exchange topic durable
        DirectExchange deadLetterExchange = new DirectExchange(properties.getDeadLetterExchange(), true, false); // DLX

        Queue disputeQueue = QueueBuilder.durable(properties.getQueueDispute())
                .withArgument(DLX_ARG, properties.getDeadLetterExchange())
                .withArgument(DLQ_ROUTING_ARG, properties.getQueueDispute())
                .build();

        Queue disputeDlq = QueueBuilder.durable(properties.dlqDispute()).build(); // DLQ de la cola de disputa

        Binding disputeBinding = BindingBuilder.bind(disputeQueue)
                .to(exchange)
                .with(properties.getRoutingKeyDisputeUpdated()); // reservation.dispute.updated

        Binding disputeDlqBinding = BindingBuilder.bind(disputeDlq)
                .to(deadLetterExchange)
                .with(properties.getQueueDispute()); // rk igual al nombre de la cola origen

        return new Declarables(exchange, deadLetterExchange, disputeQueue, disputeDlq, disputeBinding, disputeDlqBinding);
    }
}
