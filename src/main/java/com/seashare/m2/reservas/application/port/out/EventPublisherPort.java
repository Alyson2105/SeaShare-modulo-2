package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;

/**
 * Puerto de salida para publicar un mensaje del outbox en el broker AMQP (T-03).
 *
 * <p>El adaptador debe esperar el publisher confirm del broker: la publicacion
 * solo se considera exitosa con ack. Ante nack, timeout o caida del broker se
 * lanza {@link EventPublishException} y el servicio de relay agenda reintento.</p>
 */
public interface EventPublisherPort { // puerto de salida de publicacion amqp con publisher confirm

    void publish(PendingOutboxMessage message); // publica y espera confirm; lanza EventPublishException si no hay ack
}
