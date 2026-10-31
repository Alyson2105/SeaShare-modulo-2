package com.seashare.m2.reservas.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TtlPolicyTest { // pruebas unitarias para la politica de ttl

    private final TtlPolicy ttlPolicy = new TtlPolicy(); // instancia bajo prueba

    private final Instant createdAt = Instant.parse("2026-10-30T10:00:00Z"); // instante base de creacion

    @Test
    void notExpiredAtSecond899() { // valida que no este expirado al segundo 899
        Instant expiresAt = ttlPolicy.expiresAt(createdAt); // calcula expiracion esperada a 900 segundos
        Instant now899 = createdAt.plusSeconds(899); // instante justo antes de los quince minutos
        assertThat(ttlPolicy.isExpired(now899, expiresAt)).isFalse(); // verifica que aun no haya expirado
    }

    @Test
    void expiredAtSecond900() { // valida expiracion exacta al segundo 900
        Instant expiresAt = ttlPolicy.expiresAt(createdAt); // calcula expiracion esperada
        Instant now900 = createdAt.plusSeconds(900); // instante exacto de expiracion sin gracia
        assertThat(ttlPolicy.isExpired(now900, expiresAt)).isTrue(); // verifica que este expirado en el limite
    }

    @Test
    void expiredAfterSecond900() { // valida expiracion posterior al segundo 900
        Instant expiresAt = ttlPolicy.expiresAt(createdAt); // calcula expiracion esperada
        Instant now901 = createdAt.plusSeconds(901); // instante posterior a la expiracion
        assertThat(ttlPolicy.isExpired(now901, expiresAt)).isTrue(); // verifica que este expirado despues del limite
    }
}
