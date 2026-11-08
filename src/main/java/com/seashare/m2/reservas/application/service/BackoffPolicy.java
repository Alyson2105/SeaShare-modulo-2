package com.seashare.m2.reservas.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;

/**
 * Politica de reintentos del relay del outbox (T-03): maximo 5 intentos y
 * backoff exponencial 1/5/25/125 s con jitter de +/-10 %.
 *
 * <p>El {@link Random} es inyectable para que las pruebas unitarias verifiquen
 * los retardos exactos con un valor fijo (nextDouble() = 0.5 -> jitter 0).</p>
 */
@Component
public class BackoffPolicy { // politica de backoff exponencial con jitter para el relay

    private final int maxAttempts; // intentos maximos por mensaje (5 por defecto)

    private final long backoffInitialMs; // retardos base en ms: 1000 -> 1/5/25/125 s

    private final long backoffMultiplier; // multiplicador exponencial entre intentos (5)

    private final double jitterRatio; // amplitud del jitter como fraccion de la base (0.1 = +/-10 %)

    private final Random random; // fuente de aleatoriedad del jitter, inyectable en pruebas

    @Autowired
    public BackoffPolicy(@Value("${m2.scheduler.outbox.max-attempts:5}") int maxAttempts, // intentos maximos
                         @Value("${m2.scheduler.outbox.backoff-initial-ms:1000}") long backoffInitialMs, // base
                         @Value("${m2.scheduler.outbox.backoff-multiplier:5}") long backoffMultiplier, // factor
                         @Value("${m2.scheduler.outbox.jitter-ratio:0.1}") double jitterRatio) { // jitter
        this(maxAttempts, backoffInitialMs, backoffMultiplier, jitterRatio, new Random()); // random por defecto
    }

    BackoffPolicy(int maxAttempts, // constructor de prueba con Random fijo (mismo paquete)
                  long backoffInitialMs,
                  long backoffMultiplier,
                  double jitterRatio,
                  Random random) {
        this.maxAttempts = maxAttempts; // asigna intentos maximos
        this.backoffInitialMs = backoffInitialMs; // asigna base del backoff
        this.backoffMultiplier = backoffMultiplier; // asigna multiplicador
        this.jitterRatio = jitterRatio; // asigna amplitud del jitter
        this.random = random; // asigna fuente aleatoria
    }

    public boolean exhausted(int attempts) { // consulta si los intentos agotaron la politica
        return attempts >= maxAttempts; // 5º fallo -> dead
    }

    public Duration delayFor(int failedAttempt) { // calcula el retardo tras el fallo n (1-based)
        long base = backoffInitialMs; // arranca en la base (1000 ms)
        for (int i = 1; i < failedAttempt; i++) { // multiplica por intentos previos
            base *= backoffMultiplier; // 1000 -> 5000 -> 25000 -> 125000
        }
        double jitter = (random.nextDouble() * 2 - 1) * jitterRatio; // jitter uniforme en [-ratio, +ratio]
        return Duration.ofMillis(Math.round(base * (1 + jitter))); // aplica jitter sobre la base
    }

    public int maxAttempts() { // expone el tope de intentos para el log del servicio
        return maxAttempts; // retorna maxAttempts
    }
}
