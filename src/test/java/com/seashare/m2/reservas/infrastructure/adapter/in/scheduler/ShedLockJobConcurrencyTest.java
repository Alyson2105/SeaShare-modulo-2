package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.port.in.ExpireReservationsUseCase;
import com.seashare.m2.shared.PostgresTestContainers;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = { // sin scheduling automatico: el test invoca el job a mano
        "m2.scheduler.ttl.enabled=false",
        "m2.scheduler.outbox.enabled=false"})
@Import({PostgresTestContainers.class, ShedLockJobConcurrencyTest.TestConfig.class})
class ShedLockJobConcurrencyTest { // T-05: dos ejecuciones simultaneas del mismo job, una sola procede

    @Autowired
    private TestLockedJob job; // job aislado con @SchedulerLock y nombre propio

    @MockitoSpyBean
    private ExpireReservationsUseCase expireReservationsUseCase; // caso de uso espiado para bloquear la primera ejecucion

    @Test
    void dosEjecucionesSimultaneasSoloUnaProcede() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1); // avisa que la primera ejecucion entro al caso de uso
        CountDownLatch releaseFirst = new CountDownLatch(1); // mantiene tomado el lock mientras corre la segunda
        AtomicInteger executions = new AtomicInteger(); // cuenta ejecuciones reales del caso de uso

        doAnswer(invocation -> { // bloquea la primera ejecucion para forzar la concurrencia
            executions.incrementAndGet(); // cuenta la ejecucion que obtuvo el lock
            firstStarted.countDown(); // ya esta dentro del caso de uso
            releaseFirst.await(10, TimeUnit.SECONDS); // espera mientras la segunda intenta entrar
            return 1; // una reserva procesada
        }).when(expireReservationsUseCase).execute();
        job.setAction(() -> expireReservationsUseCase.execute()); // el job llama al spy en tiempo de ejecucion

        ExecutorService pool = Executors.newFixedThreadPool(2); // dos instancias simuladas del job
        try {
            Future<?> first = pool.submit(job::run); // primera ejecucion: toma el lock y se bloquea
            assertThat(firstStarted.await(10, TimeUnit.SECONDS)).isTrue(); // confirma que el lock esta tomado

            Future<?> second = pool.submit(job::run); // segunda ejecucion: debe saltarse el ciclo
            second.get(10, TimeUnit.SECONDS); // retorna sin bloqueo porque ShedLock no le da el lock
            assertThat(executions).hasValue(1); // solo una ejecucion llego al caso de uso

            releaseFirst.countDown(); // deja terminar la primera
            first.get(10, TimeUnit.SECONDS); // espera el cierre limpio
        } finally {
            releaseFirst.countDown(); // libera el latch aunque falle una asercion
            pool.shutdownNow(); // corta los hilos de prueba
        }

        assertThat(executions).hasValue(1); // sigue habiendo una sola ejecucion real
    }

    static class TestLockedJob { // job de prueba con nombre de lock propio

        private volatile Runnable action = () -> { }; // accion a ejecutar bajo el lock

        void setAction(Runnable action) { // configura la accion desde la prueba
            this.action = action; // asigna la accion
        }

        @SchedulerLock(name = "shedLockJobConcurrencyTest", lockAtMostFor = "PT1M", lockAtLeastFor = "PT1S")
        public void run() { // metodo protegido por ShedLock
            action.run(); // ejecuta la accion configurada
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig { // registra el job de prueba sin depender del scheduler de produccion

        @Bean
        TestLockedJob testLockedJob() { // bean proxificado por ShedLock
            return new TestLockedJob(); // sin dependencias inyectadas antes del spy
        }
    }
}
