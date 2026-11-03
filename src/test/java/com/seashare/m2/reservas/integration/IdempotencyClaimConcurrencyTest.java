package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IdempotencyClaimConcurrencyTest { // prueba de concurrencia real: dos transacciones compiten por la misma clave

    @Autowired
    private IdempotencyPort idempotencyPort; // puerto bajo prueba

    @Autowired
    private PlatformTransactionManager transactionManager; // gestor de transacciones para commits reales por hilo

    @Test
    void onlyOneConcurrentClaimSucceeds() throws Exception { // exactamente uno de los dos hilos obtiene el reclamo
        String key = UUID.randomUUID().toString(); // clave unica por ejecucion
        String endpoint = "CONCURRENCY_" + UUID.randomUUID(); // endpoint unico por ejecucion
        Instant now = Instant.now(); // mismo instante para ambos contendientes
        Duration window = Duration.ofSeconds(60); // ventana identica a la del caso de uso
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager); // transaccion propia por hilo

        ExecutorService executor = Executors.newFixedThreadPool(2); // pool de dos contendientes
        CountDownLatch start = new CountDownLatch(1); // barrera para arrancar ambos a la vez
        try {
            List<Future<Boolean>> futures = new ArrayList<>(); // resultados asincronos
            for (int i = 0; i < 2; i++) { // lanza dos intentos concurrentes
                futures.add(executor.submit(() -> { // cada intento corre en su propio hilo
                    start.await(); // espera la senal de arranque simultaneo
                    return transactionTemplate.execute(status -> idempotencyPort.tryClaim(key, endpoint, now, window)); // commit real
                }));
            }
            start.countDown(); // libera ambos hilos a la vez

            List<Boolean> results = new ArrayList<>(); // recolecta los reclamos
            for (Future<Boolean> future : futures) { // espera cada resultado
                results.add(future.get(30, TimeUnit.SECONDS)); // timeout defensivo
            }

            assertThat(results).containsExactlyInAnyOrder(true, false); // solo uno gana el reclamo
        } finally {
            executor.shutdownNow(); // libera el pool
        }
    }
}
