package com.seashare.m2.reservas.domain.service;

import java.time.Duration;
import java.time.Instant;

public class TtlPolicy { // politica de ttl para expiracion de reservas

    public static final Duration TTL = Duration.ofMinutes(15); // constante de duracion ttl de quince minutos

    public Instant expiresAt(Instant createdAt) { // calcula la marca temporal de expiracion
        return createdAt.plus(TTL); // suma la duracion de ttl al instante inicial
    }

    public boolean isExpired(Instant now, Instant expiresAt) { // evalua si el instante actual supero o igualo la expiracion
        return !now.isBefore(expiresAt); // retorna verdadero si now es igual o posterior a expiresAt
    }
}
