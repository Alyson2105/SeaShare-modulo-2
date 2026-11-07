package com.seashare.m2.reservas.application.port.in;

/**
 * Caso de uso de entrada del relay del outbox (T-03).
 *
 * <p>Publica los mensajes pendientes por destino (AMQP con publisher confirm,
 * M1_HTTP via {@code FleetStatusNotificationPort}), aplica la politica de
 * reintentos (maximo 5 intentos, backoff 1/5/25/125 s con jitter) y marca
 * {@code published_at} solo tras el confirm del broker.</p>
 */
public interface PublishPendingOutboxUseCase { // caso de uso que descarga el outbox pendiente

    int execute(); // procesa el lote pendiente y retorna cuantos mensajes se intentaron
}
