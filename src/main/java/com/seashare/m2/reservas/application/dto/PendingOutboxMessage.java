package com.seashare.m2.reservas.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje pendiente del outbox leido por el relay (T-03).
 *
 * <p>Vista publica de {@code OutboxMessageEntity}: la entidad y su repositorio
 * JPA son package-private en infraestructura, asi que la capa de aplicacion
 * trabaja con este record.</p>
 *
 * @param id            clave primaria de la fila outbox_message
 * @param eventId       identificador unico del evento (viaja como Message-Id)
 * @param destination   destino del mensaje: AMQP o M1_HTTP
 * @param exchange      exchange de RabbitMQ (null en M1_HTTP)
 * @param routingKey    routing key de RabbitMQ (null en M1_HTTP)
 * @param payload       cuerpo json del evento
 * @param createdAt     instante de creacion de la fila
 * @param attempts      intentos consumidos hasta ahora
 */
public record PendingOutboxMessage( // lectura inmutable de un mensaje outbox para el relay
        UUID id,
        UUID eventId,
        String destination,
        String exchange,
        String routingKey,
        String payload,
        Instant createdAt,
        int attempts) {
}
