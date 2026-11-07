package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
class IdempotencyKeyId implements Serializable { // clave compuesta (key, endpoint) de la tabla idempotency_key

    @Column(name = "key", nullable = false, length = 150)
    private String idempotencyKey; // clave idempotente enviada por el cliente; no se llama key por la funcion KEY() de hql

    @Column(name = "endpoint", nullable = false, length = 150)
    private String endpoint; // endpoint logico que reclama la clave

    IdempotencyKeyId() { // constructor sin argumentos requerido por jpa
    }

    IdempotencyKeyId(String idempotencyKey, String endpoint) { // construye la clave compuesta completa
        this.idempotencyKey = idempotencyKey; // asigna la clave idempotente
        this.endpoint = endpoint; // asigna el endpoint
    }

    String getIdempotencyKey() { // obtiene la clave idempotente
        return idempotencyKey; // retorna la clave
    }

    String getEndpoint() { // obtiene el endpoint
        return endpoint; // retorna el endpoint
    }

    @Override
    public boolean equals(Object other) { // compara por valor de ambos componentes
        if (this == other) { // verifica identidad de referencia
            return true; // misma instancia
        }
        if (!(other instanceof IdempotencyKeyId that)) { // descarta tipos distintos
            return false; // no es una clave compuesta
        }
        return Objects.equals(idempotencyKey, that.idempotencyKey) && Objects.equals(endpoint, that.endpoint); // igualdad de componentes
    }

    @Override
    public int hashCode() { // calcula hash coherente con equals
        return Objects.hash(idempotencyKey, endpoint); // combina ambos componentes
    }
}
