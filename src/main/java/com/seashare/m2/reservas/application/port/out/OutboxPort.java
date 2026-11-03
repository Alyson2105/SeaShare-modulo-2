package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.StatusChangedEvent;

public interface OutboxPort { // puerto para encolar eventos en outbox

    void enqueue(StatusChangedEvent event); // encola un evento en la tabla outbox
}
