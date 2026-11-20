package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.in.PublishPendingOutboxUseCase;
import com.seashare.m2.reservas.application.port.out.EventPublisherPort;
import com.seashare.m2.reservas.application.port.out.FleetNotificationOutcome;
import com.seashare.m2.reservas.application.port.out.FleetStatusNotificationPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.shared.domain.ClockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Relay del outbox (T-03): descarga los mensajes pendientes y los despacha por
 * destino. Reglas:
 *
 * <ul>
 *   <li>AMQP: publica con publisher confirm y marca {@code published_at} SOLO tras el ack.</li>
 *   <li>M1_HTTP: llama a {@link FleetStatusNotificationPort}; nunca hace HTTP en el relay (D-01).
 *       4xx -> dead directo (D-05); 5xx/timeout -> reintento con backoff.</li>
 *   <li>Maximo 5 intentos, backoff 1/5/25/125 s con jitter; 5º fallo -> {@code dead=true} + alerta.</li>
 *   <li>Cada mensaje se procesa en su propia transaccion: el fallo de uno no frena a los demas.</li>
 *   <li>Nunca propaga excepciones al job (el job tambien las atrapa).</li>
 *   <li>Todo el tiempo viene de {@link ClockPort}; nunca se llama Instant.now().</li>
 * </ul>
 */
@Service
public class PublishPendingOutboxService implements PublishPendingOutboxUseCase { // servicio relay del outbox

    private static final Logger log = LoggerFactory.getLogger(PublishPendingOutboxService.class); // logger del relay

    static final String DESTINATION_AMQP = "AMQP"; // destino amqp hacia M3

    static final String DESTINATION_M1_HTTP = "M1_HTTP"; // destino http hacia M1

    private static final int MAX_ERROR_LENGTH = 1000; // tope de caracteres guardados en last_error

    private final OutboxPort outboxPort; // lectura y marcado de mensajes pendientes

    private final EventPublisherPort eventPublisherPort; // publicacion amqp con publisher confirm

    private final FleetStatusNotificationPort fleetStatusNotificationPort; // notificacion http a M1 (fake en M2)

    private final ClockPort clockPort; // reloj unico de la aplicacion

    private final TransactionTemplate transactionTemplate; // transaccion por mensaje para aislar fallos

    private final BackoffPolicy backoffPolicy; // politica de reintentos con jitter

    private final int batchSize; // maximo de mensajes por ciclo

    private final long leaseMillis; // tiempo que un mensaje queda reclamado antes de reintentarse

    public PublishPendingOutboxService(OutboxPort outboxPort, // constructor con inyeccion de puertos
                                       EventPublisherPort eventPublisherPort,
                                       FleetStatusNotificationPort fleetStatusNotificationPort,
                                       ClockPort clockPort,
                                       TransactionTemplate transactionTemplate,
                                       BackoffPolicy backoffPolicy,
                                       @Value("${m2.scheduler.outbox.batch-size:50}") int batchSize,
                                       @Value("${m2.scheduler.outbox.lease-ms:30000}") long leaseMillis) {
        this.outboxPort = outboxPort; // asigna puerto outbox
        this.eventPublisherPort = eventPublisherPort; // asigna publicador amqp
        this.fleetStatusNotificationPort = fleetStatusNotificationPort; // asigna notificador de M1
        this.clockPort = clockPort; // asigna reloj
        this.transactionTemplate = transactionTemplate; // asigna plantilla transaccional
        this.backoffPolicy = backoffPolicy; // asigna politica de backoff
        this.batchSize = batchSize; // asigna tamano del lote
        this.leaseMillis = leaseMillis; // asigna duracion del lease
    }

    @Override
    public int execute() { // procesa hasta batchSize mensajes: reclama en una TX y llama fuera de ella
        int processed = 0; // contador de mensajes intentados
        for (int i = 0; i < batchSize; i++) { // cada mensaje se reclama y procesa por separado
            PendingOutboxMessage message; // mensaje reclamado
            try {
                message = claimNext(); // TX corta: bloquea y arrienda el mensaje
            } catch (Exception e) { // nunca propaga al job
                log.error("Outbox relay: fallo inesperado al reclamar: {}", e.getMessage(), e); // alerta y corta el ciclo
                break; // deja el resto para el siguiente ciclo
            }
            if (message == null) { // no quedan pendientes
                break; // termina el ciclo
            }
            try {
                processOne(message); // llamada externa FUERA de la transaccion
            } catch (Exception e) { // un fallo de marcado no frena a los demas mensajes
                log.error("Outbox relay: fallo inesperado al procesar {}: {}", message.eventId(), e.getMessage(), e);
            }
            processed++; // cuenta el mensaje como intentado
        }
        if (processed > 0) { // solo loguea si hubo actividad
            log.info("Outbox relay: ciclo completado, {} mensaje(s) procesado(s)", processed); // resumen del ciclo
        }
        return processed; // retorna cuantos mensajes se intentaron
    }

    private PendingOutboxMessage claimNext() { // reclama y arrienda un mensaje en una transaccion corta
        return transactionTemplate.execute(status -> {
            Instant now = clockPort.now(); // hora unica del reclamo
            List<PendingOutboxMessage> pending = outboxPort.findPending(now, 1); // SKIP LOCKED: un mensaje por instancia
            if (pending.isEmpty()) { // sin mensajes vencidos
                return null; // no hay nada que procesar
            }
            PendingOutboxMessage message = pending.get(0); // mensaje reclamado
            outboxPort.lease(message.id(), now.plusMillis(leaseMillis)); // lo oculta de otras instancias hasta el lease
            return message; // el bloqueo se libera al commit de esta transaccion
        });
    }

    private void processOne(PendingOutboxMessage message) { // despacho por destino, fuera de la transaccion
        if (DESTINATION_AMQP.equals(message.destination())) { // destino amqp hacia M3
            publishAmqp(message); // publica con confirm
        } else if (DESTINATION_M1_HTTP.equals(message.destination())) { // destino http hacia M1
            notifyModuleOne(message); // delega en el puerto de flota, sin http en el relay
        } else { // destino fuera del catalogo: nunca tendra exito
            inTransaction(() -> markDead(message, message.attempts() + 1, "Destino desconocido: " + message.destination())); // dead directo
        }
    }

    private void publishAmqp(PendingOutboxMessage message) { // publicacion amqp con esperas de confirm
        try {
            eventPublisherPort.publish(message); // publica y espera el confirm del broker (sin transaccion abierta)
            inTransaction(() -> outboxPort.markPublished(message.id(), clockPort.now())); // TX corta: marca published_at
            log.debug("Outbox relay: evento {} publicado y confirmado por el broker", message.eventId()); // traza
        } catch (RuntimeException e) { // broker sin confirm (nack, timeout o caida): reintento (M3)
            registerFailure(message, "AMQP sin confirmacion: " + errorMessage(e)); // agenda reintento o dead
        }
    }

    private void notifyModuleOne(PendingOutboxMessage message) { // notificacion a M1 via puerto
        try {
            FleetNotificationOutcome outcome = fleetStatusNotificationPort.notifyStatus(message); // llamada fuera de transaccion
            switch (outcome) { // clasifica el resultado
                case DELIVERED -> { // 2xx: M1 acepto
                    inTransaction(() -> outboxPort.markPublished(message.id(), clockPort.now())); // TX corta: marca publicado
                    log.debug("Outbox relay: notificacion M1 {} entregada", message.eventId()); // traza
                }
                case RETRYABLE_FAILURE -> registerFailure(message, "M1_HTTP fallo transitorio (5xx o timeout)"); // reintento
                case PERMANENT_FAILURE -> inTransaction(() -> markDead( // TX corta
                        message, message.attempts() + 1, "M1_HTTP 4xx permanente (D-05)")); // dead directo
            }
        } catch (RuntimeException e) { // excepcion del adaptador: se trata como fallo transitorio
            registerFailure(message, "M1_HTTP error inesperado: " + errorMessage(e)); // agenda reintento
        }
    }

    private void registerFailure(PendingOutboxMessage message, String error) { // guarda intento y proximo retroceso
        int attempts = message.attempts() + 1; // primer fallo -> intento 1
        String detail = truncate(error); // recorta el error para last_error
        if (backoffPolicy.exhausted(attempts)) { // 5º fallo agotado
            inTransaction(() -> markDead(message, attempts, detail)); // dead + alerta
            return; // sin proximo intento
        }
        Duration delay = backoffPolicy.delayFor(attempts); // backoff 1/5/25/125 s con jitter
        Instant nextAttemptAt = clockPort.now().plus(delay); // hora del proximo intento desde el reloj
        inTransaction(() -> outboxPort.markFailed(message.id(), attempts, nextAttemptAt, detail, false)); // TX corta
        log.warn("Outbox relay: evento {} fallo {}/{}; reintento en {} ms: {}",
                message.eventId(), attempts, backoffPolicy.maxAttempts(), delay.toMillis(), detail); // aviso
    }

    private void markDead(PendingOutboxMessage message, int attempts, String error) { // marca evento muerto
        outboxPort.markFailed(message.id(), attempts, null, truncate(error), true); // dead=true sin proximo intento
        log.error("ALERTA outbox relay: evento {} marcado dead tras {} intento(s): {}",
                message.eventId(), attempts, truncate(error)); // alerta critica
    }

    private void inTransaction(Runnable action) { // ejecuta una escritura corta en su propia transaccion
        transactionTemplate.execute(status -> {
            action.run(); // marca el resultado del mensaje
            return null;
        });
    }

    private String errorMessage(Exception e) { // mensaje de error seguro para el log y last_error
        String message = e.getMessage(); // puede ser nulo
        return message != null ? message : e.getClass().getSimpleName(); // usa la clase si no hay mensaje
    }

    private String truncate(String error) { // recorta errores demasiado largos
        if (error == null) { // sin mensaje
            return "sin detalle"; // valor por defecto
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH); // tope 1000
    }
}
