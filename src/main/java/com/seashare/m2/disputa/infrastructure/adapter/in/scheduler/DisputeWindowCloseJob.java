package com.seashare.m2.disputa.infrastructure.adapter.in.scheduler;

import com.seashare.m2.disputa.application.port.in.CloseExpiredDisputesUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que cierra las disputas vencidas sin reclamo (T-18b).
 *
 * <p>Solo invoca el caso de uso; sin logica de negocio aqui. Se activa con
 * {@code m2.scheduler.dispute.enabled=true} (desactivado por defecto para que la
 * suite no mute datos de disputa en segundo plano). Nunca propaga excepciones.</p>
 */
@Component
@ConditionalOnProperty(name = "m2.scheduler.dispute.enabled", havingValue = "true") // desactivable por propiedad
public class DisputeWindowCloseJob { // adaptador de entrada: job del cierre automatico de la ventana

    private static final Logger log = LoggerFactory.getLogger(DisputeWindowCloseJob.class); // logger del job

    private final CloseExpiredDisputesUseCase closeExpiredDisputesUseCase; // caso de uso al que delega

    public DisputeWindowCloseJob(CloseExpiredDisputesUseCase closeExpiredDisputesUseCase) {
        this.closeExpiredDisputesUseCase = closeExpiredDisputesUseCase;
    }

    @Scheduled(fixedDelayString = "${m2.scheduler.dispute.fixed-delay-ms:30000}") // intervalo configurable; default 30 s
    @SchedulerLock(name = "disputeWindowCloseJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1S") // T-05: una sola instancia
    public void run() { // dispara el cierre delegando en el caso de uso
        try {
            closeExpiredDisputesUseCase.execute(); // barrido durable sobre window_end
        } catch (Exception e) { // nunca propaga: el scheduler no debe detenerse
            log.error("Cierre de disputas: ciclo fallido sin propagar: {}", e.getMessage(), e);
        }
    }
}
