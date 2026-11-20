package com.seashare.m2.reservas.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackoffPolicyTest { // prueba la politica de backoff exacta 1/5/25/125 s con jitter controlable

    private Random random; // random fijo para jitter determinista

    private BackoffPolicy policy; // politica bajo prueba

    @BeforeEach
    void setUp() { // crea la politica con jitter exactamente cero
        random = mock(Random.class); // random simulado
        when(random.nextDouble()).thenReturn(0.5); // 0.5 -> jitter (0.5*2-1)*0.1 = 0 -> retardo base exacto
        policy = new BackoffPolicy(5, 1000, 5, 0.1, random); // 5 intentos, base 1 s, multiplicador 5, +/-10 %
    }

    @Test
    void delayForProduceExactBackoffOneFiveTwentyFiveHundredTwentyFive() { // 1/5/25/125 s exactos
        assertThat(policy.delayFor(1)).isEqualTo(Duration.ofSeconds(1)); // primer fallo -> 1 s
        assertThat(policy.delayFor(2)).isEqualTo(Duration.ofSeconds(5)); // segundo fallo -> 5 s
        assertThat(policy.delayFor(3)).isEqualTo(Duration.ofSeconds(25)); // tercer fallo -> 25 s
        assertThat(policy.delayFor(4)).isEqualTo(Duration.ofSeconds(125)); // cuarto fallo -> 125 s
    }

    @Test
    void jitterVaryWithinConfiguredRatio() { // el jitter respeta +/-10 % de la base
        when(random.nextDouble()).thenReturn(0.0); // extremo inferior -> -10 %
        assertThat(policy.delayFor(1)).isEqualTo(Duration.ofMillis(900)); // 1000 ms - 10 %

        when(random.nextDouble()).thenReturn(1.0); // extremo superior -> +10 %
        assertThat(policy.delayFor(1)).isEqualTo(Duration.ofMillis(1100)); // 1000 ms + 10 %
    }

    @Test
    void exhaustedOnlyAfterMaxAttempts() { // dead a partir del 5º intento
        assertThat(policy.exhausted(4)).isFalse(); // 4 intentos sigue vivo
        assertThat(policy.exhausted(5)).isTrue(); // 5º intento agota la politica
    }
}
