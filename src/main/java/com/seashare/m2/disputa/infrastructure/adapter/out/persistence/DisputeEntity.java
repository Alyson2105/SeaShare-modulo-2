package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import com.seashare.m2.disputa.domain.model.DisputeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_guarantee")
class DisputeEntity { // mapeo jpa de la disputa de garantia (V2 + V7)

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // identificador generado por el dominio

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId; // reserva asociada (UNIQUE en V2)

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId; // propietario de la reserva (V7)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DisputeStatus status; // PENDING, REJECTED o ACCEPTED

    @Column(name = "window_start", nullable = false, updatable = false)
    private Instant windowStart; // inicio de la ventana de 24 h

    @Column(name = "window_end", nullable = false, updatable = false)
    private Instant windowEnd; // fin de la ventana de 24 h

    @Column(name = "has_claim", nullable = false)
    private boolean hasClaim; // unico reclamo registrado

    @Column(name = "reason")
    private String reason; // motivo informativo, nunca viaja a M3

    @Column(name = "resolved_by")
    private UUID resolvedBy; // admin que resolvio; nulo en cierre de sistema

    @Column(name = "resolved_at")
    private Instant resolvedAt; // instante de la resolucion

    @Version
    @Column(name = "version", nullable = false)
    private long version; // bloqueo optimista (CU-17 plan 7.3)

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public boolean isHasClaim() {
        return hasClaim;
    }

    public void setHasClaim(boolean hasClaim) {
        this.hasClaim = hasClaim;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public long getVersion() {
        return version;
    }
}
