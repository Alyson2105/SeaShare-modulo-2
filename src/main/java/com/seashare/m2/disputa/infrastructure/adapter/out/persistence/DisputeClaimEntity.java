package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_claim")
class DisputeClaimEntity { // mapeo jpa del unico reclamo por disputa (V2)

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // identificador generado por el dominio

    @Column(name = "dispute_id", nullable = false, updatable = false)
    private UUID disputeId; // disputa reclamada (UNIQUE en V2)

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId; // propietario que radico el reclamo

    @Column(name = "description", nullable = false, updatable = false)
    private String description; // relato factico

    @Column(name = "category", length = 50, updatable = false)
    private String category; // valor snake_case del contrato

    @Column(name = "evidence_urls", updatable = false)
    private String evidenceUrls; // arreglo json de enlaces

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt; // marca oficial de radicacion

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

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(String evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
