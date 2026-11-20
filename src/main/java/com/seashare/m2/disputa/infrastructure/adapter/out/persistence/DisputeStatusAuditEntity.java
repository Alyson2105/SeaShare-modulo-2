package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_status_audit")
class DisputeStatusAuditEntity { // mapeo jpa del historial inmutable de estados (V7)

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "dispute_id", nullable = false, updatable = false)
    private UUID disputeId;

    @Column(name = "from_status", length = 20, updatable = false)
    private String fromStatus; // nulo en la creacion

    @Column(name = "to_status", length = 20, nullable = false, updatable = false)
    private String toStatus;

    @Column(name = "actor_type", length = 20, nullable = false, updatable = false)
    private String actorType; // SYSTEM o ADMIN

    @Column(name = "actor_id", updatable = false)
    private UUID actorId; // nulo cuando el actor es el sistema

    @Column(name = "reason", updatable = false)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(UUID disputeId) {
        this.disputeId = disputeId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
