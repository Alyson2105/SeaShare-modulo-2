package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "idempotency_key")
class IdempotencyKeyEntity { // entidad jpa para la tabla idempotency_key

    @EmbeddedId
    private IdempotencyKeyId id; // clave primaria compuesta (key, endpoint)

    @Column(name = "response_ref")
    private String responseRef; // referencia textual al resultado ya procesado

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt; // marca temporal del ultimo reclamo

    IdempotencyKeyEntity() { // constructor sin argumentos requerido por jpa
    }

    IdempotencyKeyId getId() { // obtiene la clave compuesta
        return id; // retorna el id
    }

    void setId(IdempotencyKeyId id) { // asigna la clave compuesta
        this.id = id; // establece el id
    }

    String getResponseRef() { // obtiene la referencia del resultado
        return responseRef; // retorna responseRef
    }

    void setResponseRef(String responseRef) { // asigna la referencia del resultado
        this.responseRef = responseRef; // establece responseRef
    }

    OffsetDateTime getCreatedAt() { // obtiene la marca temporal del reclamo
        return createdAt; // retorna createdAt
    }

    void setCreatedAt(OffsetDateTime createdAt) { // asigna la marca temporal del reclamo
        this.createdAt = createdAt; // establece createdAt
    }
}
