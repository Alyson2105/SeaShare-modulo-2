package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;

/**
 * Puerto de salida para notificar estado operativo a M1 por HTTP PATCH (T-03, D-01).
 *
 * <p>El relay NUNCA hace HTTP directamente: delega en este puerto. El adaptador
 * de este hito es FAKE (solo loguea y responde OK); el cliente real de M1
 * cambiara unicamente el adaptador sin tocar el servicio de relay.</p>
 */
public interface FleetStatusNotificationPort { // puerto de salida de notificacion de estado operativo a M1

    FleetNotificationOutcome notifyStatus(PendingOutboxMessage message); // envia la notificacion y clasifica el resultado
}
