package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.port.in.PublishPendingOutboxUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que descarga el outbox pendiente (T-03).
 * Solo invoca {@link PublishPendingOutboxUseCase}; sin logica de negocio aqui.
 *
 * <p>Se activa con {@code m2.scheduler.outbox.enabled=true} (por defecto esta
 * desactivado en application.yaml; activo en los perfiles dev y prod). El job
 * nunca propaga excepciones: cualquier fallo se registra y se reintenta en el
 * siguiente ciclo.</p>
 *
 * // TODO (T-05): agregar ShedLock antes de desplegar con mas de una réplica.
 */
@Component
@ConditionalOnProperty(name = "m2.scheduler.outbox.enabled", havingValue = "true") // desactivable por propiedad
public class OutboxRelayJob { // adaptador de entrada: job programado del relay del outbox

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class); // logger del job

    private final PublishPendingOutboxUseCase publishPendingOutboxUseCase; // caso de uso al que delega

    public OutboxRelayJob(PublishPendingOutboxUseCase publishPendingOutboxUseCase) { // constructor con inyeccion
        this.publishPendingOutboxUseCase = publishPendingOutboxUseCase; // asigna caso de uso
    }

    @Scheduled(fixedDelayString = "${m2.scheduler.outbox.fixed-delay-ms:5000}") // intervalo configurable; default 5 s
    public void run() { // dispara el relay delegando en el caso de uso
        try {
            publishPendingOutboxUseCase.execute(); // delega todo el trabajo en el caso de uso
        } catch (Exception e) { // nunca propaga: el scheduler no debe detenerse
            log.error("Outbox relay: ciclo fallido sin propagar: {}", e.getMessage(), e); // alerta y sigue
        }
    }
}
