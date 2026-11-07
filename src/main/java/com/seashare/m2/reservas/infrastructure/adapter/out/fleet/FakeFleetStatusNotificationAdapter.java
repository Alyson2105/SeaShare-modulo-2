package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.FleetNotificationOutcome;
import com.seashare.m2.reservas.application.port.out.FleetStatusNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador FAKE de notificacion de estado operativo a M1 (T-03, D-01).
 *
 * <p>No hace HTTP: solo registra el evento en log y responde OK. La integracion
 * real (PATCH a M1) cambiara unicamente este adaptador por uno HTTP, sin tocar
 * el relay ni el caso de uso.</p>
 */
@Component
public class FakeFleetStatusNotificationAdapter implements FleetStatusNotificationPort { // fake de notificacion a M1

    private static final Logger log = LoggerFactory.getLogger(FakeFleetStatusNotificationAdapter.class); // logger

    @Override
    public FleetNotificationOutcome notifyStatus(PendingOutboxMessage message) { // registra y responde OK
        log.info("FAKE M1_HTTP: notificacion de estado operativo {} (evento {}, payload {} bytes)",
                message.id(), message.eventId(), message.payload().length()); // solo loguea la notificacion
        return FleetNotificationOutcome.DELIVERED; // siempre responde OK mientras no exista cliente HTTP real
    }
}
