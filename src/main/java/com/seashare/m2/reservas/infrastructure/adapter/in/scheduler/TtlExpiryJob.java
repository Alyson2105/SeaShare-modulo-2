package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.port.in.ExpireReservationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que dispara la expiración por TTL de reservas en INITIATED.
 * Solo invoca el caso de uso; sin lógica de negocio aquí.
 *
 * // TODO (D-15): agregar ShedLock antes de desplegar con más de una réplica para evitar
 * //              ejecuciones concurrentes del mismo job en un entorno multi-instancia.
 */
@Component
public class TtlExpiryJob { // adaptador de entrada: job programado de expiración por TTL

    private static final Logger log = LoggerFactory.getLogger(TtlExpiryJob.class); // logger del job

    private final ExpireReservationsUseCase expireReservationsUseCase; // caso de uso al que delega todo el trabajo

    public TtlExpiryJob(ExpireReservationsUseCase expireReservationsUseCase) { // constructor con inyección del caso de uso
        this.expireReservationsUseCase = expireReservationsUseCase; // asigna caso de uso
    }

    @Scheduled(fixedDelayString = "${m2.scheduler.ttl.fixed-delay-ms:30000}") // intervalo configurable entre ejecuciones; default 30 s
    public void run() { // dispara la expiración delegando en el caso de uso
        log.debug("TTL scheduler: iniciando ciclo de expiración"); // traza de inicio del ciclo
        int expired = expireReservationsUseCase.execute(); // ejecuta el caso de uso y obtiene el número procesado
        if (expired > 0) { // solo loguea si hubo actividad
            log.info("TTL scheduler: ciclo completado, {} reserva(s) expirada(s)", expired); // resumen del ciclo
        }
    }
}
