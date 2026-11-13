package com.seashare.m2.disputa.infrastructure.adapter.out.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila del outbox transaccional vista desde el contexto disputa.
 *
 * <p>Mapea solo las columnas que necesita el INSERT; el resto (published_at,
 * attempts, dead, last_error) lo gestiona el relay de reservas con sus propias
 * entidades. La tabla es tecnica y compartida por el unico deployable (D-01).</p>
 */
@Entity
@Table(name = "outbox_message")
class DisputeOutboxEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave de la fila

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId; // identificador unico del evento (Message-Id)

    @Column(name = "destination", length = 20, nullable = false)
    private String destination; // AMQP

    @Column(name = "exchange", length = 100)
    private String exchange; // exchange topic de M3

    @Column(name = "routing_key", length = 150)
    private String routingKey; // reservation.dispute.updated

    @Column(name = "payload", nullable = false)
    private String payload; // cuerpo json minimalista

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // instante del cambio de estado

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt; // primer intento del relay

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }
}
