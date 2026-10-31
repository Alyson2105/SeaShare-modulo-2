package com.seashare.m2.reservas.application.port.out;

public interface OutboxPort { // puerto para encolar eventos en outbox

    void enqueue(StatusChangedEvent event); // encola un evento en la tabla outbox
}
