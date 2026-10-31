package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_message")
public class OutboxMessageEntity { // entidad jpa para mensajes outbox

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId; // identificador unico del evento

    @Column(name = "destination", length = 20, nullable = false)
    private String destination; // destino del mensaje

    @Column(name = "exchange", length = 100)
    private String exchange; // exchange rabbitmq

    @Column(name = "routing_key", length = 150)
    private String routingKey; // routing key rabbitmq

    @Column(name = "payload", nullable = false)
    private String payload; // cuerpo json del evento

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // marca temporal de creacion

    @Column(name = "published_at")
    private Instant publishedAt; // marca temporal de publicacion confirmada

    @Column(name = "attempts", nullable = false)
    private int attempts = 0; // contador de reintentos

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt; // proximo intento de envio

    @Column(name = "dead", nullable = false)
    private boolean dead = false; // indicador de descarte por reintentos agotados

    @Column(name = "last_error")
    private String lastError; // descripcion del ultimo fallo

    public OutboxMessageEntity() { // constructor sin argumentos requerido por jpa
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public void setId(UUID id) { // asigna el id
        this.id = id; // establece id
    }

    public UUID getEventId() { // obtiene el id del evento
        return eventId; // retorna eventId
    }

    public void setEventId(UUID eventId) { // asigna el id del evento
        this.eventId = eventId; // establece eventId
    }

    public String getDestination() { // obtiene el destino
        return destination; // retorna destination
    }

    public void setDestination(String destination) { // asigna el destino
        this.destination = destination; // establece destination
    }

    public String getExchange() { // obtiene el exchange
        return exchange; // retorna exchange
    }

    public void setExchange(String exchange) { // asigna el exchange
        this.exchange = exchange; // establece exchange
    }

    public String getRoutingKey() { // obtiene la routing key
        return routingKey; // retorna routingKey
    }

    public void setRoutingKey(String routingKey) { // asigna la routing key
        this.routingKey = routingKey; // establece routingKey
    }

    public String getPayload() { // obtiene el payload
        return payload; // retorna payload
    }

    public void setPayload(String payload) { // asigna el payload
        this.payload = payload; // establece payload
    }

    public Instant getCreatedAt() { // obtiene la fecha de creacion
        return createdAt; // retorna createdAt
    }

    public void setCreatedAt(Instant createdAt) { // asigna la fecha de creacion
        this.createdAt = createdAt; // establece createdAt
    }

    public Instant getPublishedAt() { // obtiene la fecha de publicacion
        return publishedAt; // retorna publishedAt
    }

    public void setPublishedAt(Instant publishedAt) { // asigna la fecha de publicacion
        this.publishedAt = publishedAt; // establece publishedAt
    }

    public int getAttempts() { // obtiene los reintentos
        return attempts; // retorna attempts
    }

    public void setAttempts(int attempts) { // asigna los reintentos
        this.attempts = attempts; // establece attempts
    }

    public Instant getNextAttemptAt() { // obtiene el proximo intento
        return nextAttemptAt; // retorna nextAttemptAt
    }

    public void setNextAttemptAt(Instant nextAttemptAt) { // asigna el proximo intento
        this.nextAttemptAt = nextAttemptAt; // establece nextAttemptAt
    }

    public boolean isDead() { // consulta si esta descartado
        return dead; // retorna dead
    }

    public void setDead(boolean dead) { // asigna si esta descartado
        this.dead = dead; // establece dead
    }

    public String getLastError() { // obtiene el ultimo fallo
        return lastError; // retorna lastError
    }

    public void setLastError(String lastError) { // asigna el ultimo fallo
        this.lastError = lastError; // establece lastError
    }
}
