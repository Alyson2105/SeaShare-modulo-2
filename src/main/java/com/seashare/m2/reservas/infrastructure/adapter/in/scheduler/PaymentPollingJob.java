package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.port.in.PollPaymentStatusUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job del canal B de CU-13: consulta periodicamente a Modulo 3 el estado del cobro.
 * Se activa con {@code m2.scheduler.payments.enabled=true} (apagado por defecto en la
 * suite, activo en dev y prod). Nunca propaga excepciones.
 */
@Component
@ConditionalOnProperty(name = "m2.scheduler.payments.enabled", havingValue = "true")
public class PaymentPollingJob { // adaptador de entrada: consulta periodica de cobros

    private static final Logger log = LoggerFactory.getLogger(PaymentPollingJob.class);

    private final PollPaymentStatusUseCase pollPaymentStatus;

    public PaymentPollingJob(PollPaymentStatusUseCase pollPaymentStatus) {
        this.pollPaymentStatus = pollPaymentStatus;
    }

    @Scheduled(fixedDelayString = "${m2.scheduler.payments.fixed-delay-ms:5000}") // cada 5 s por defecto
    @SchedulerLock(name = "paymentPollingJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1S") // T-05
    public void run() {
        try {
            int processed = pollPaymentStatus.execute();
            if (processed > 0) {
                log.info("CU-13: ciclo de cobro completado, {} resultado(s) definitivo(s)", processed);
            }
        } catch (Exception e) { // el scheduler no debe detenerse
            log.error("CU-13: ciclo de cobro fallido sin propagar: {}", e.getMessage(), e);
        }
    }
}
