package com.seashare.m2.reservas.infrastructure.adapter.out.messaging;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.EventPublishException;
import com.seashare.m2.reservas.application.port.out.EventPublisherPort;
import com.seashare.m2.reservas.infrastructure.adapter.out.messaging.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Publicador AMQP con publisher confirm (T-03/T-04).
 *
 * <p>Requiere {@code spring.rabbitmq.publisher-confirm-type: correlate}. Publica
 * con {@code Message-Id} = {@code event_id}, {@code Content-Type} JSON y mensaje
 * persistente, y espera el confirm del broker: sin ack lanza
 * {@link EventPublishException} para que el relay agende reintento.</p>
 */
@Component
public class RabbitEventPublisherAdapter implements EventPublisherPort { // adaptador de publicacion amqp con confirm

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisherAdapter.class); // logger

    private final RabbitTemplate rabbitTemplate; // plantilla amqp con confirms habilitados

    private final MessagingProperties messagingProperties; // exchange y routing keys de application.yaml

    private final long publishTimeoutMs; // espera maxima del confirm del broker

    public RabbitEventPublisherAdapter(RabbitTemplate rabbitTemplate, // constructor con dependencias
                                       MessagingProperties messagingProperties,
                                       @Value("${m2.messaging.publish-timeout-ms:5000}") long publishTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate; // asigna plantilla
        this.messagingProperties = messagingProperties; // asigna propiedades de mensajeria
        this.publishTimeoutMs = publishTimeoutMs; // asigna timeout de confirm
    }

    @Override
    public void publish(PendingOutboxMessage message) { // publica y espera el confirm del broker
        MessageProperties properties = new MessageProperties(); // propiedades del sobre amqp
        properties.setMessageId(message.eventId().toString()); // Message-Id = event_id (deduplicacion en el consumidor)
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON); // Content-Type application/json
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT); // mensaje persistente en el broker
        Message amqpMessage = new Message(message.payload().getBytes(StandardCharsets.UTF_8), properties); // sobre
        CorrelationData correlationData = new CorrelationData(message.eventId().toString()); // correlacion del confirm
        try {
            rabbitTemplate.send(message.exchange(), message.routingKey(), amqpMessage, correlationData); // publica
            CorrelationData.Confirm confirm = // espera la confirmacion del broker
                    correlationData.getFuture().get(publishTimeoutMs, TimeUnit.MILLISECONDS); // con timeout
            if (!confirm.isAck()) { // broker rechazo el mensaje
                throw new EventPublishException("Broker rechazo el mensaje: " + confirm.getReason()); // reintento
            }
            log.debug("Publicacion AMQP confirmada para el evento {}", message.eventId()); // traza de ack
        } catch (EventPublishException e) { // rechazo del broker: ya es una falta clasificada
            throw e; // propaga tal cual
        } catch (InterruptedException e) { // interrupcion durante la espera del confirm
            Thread.currentThread().interrupt(); // restaura la bandera de interrupcion
            throw new EventPublishException("Espera de confirm AMQP interrumpida", e); // reintento
        } catch (Exception e) { // timeout, caida de conexion u otro error del broker
            throw new EventPublishException("Sin confirm del broker: " + e.getMessage(), e); // reintento
        }
    }
}
