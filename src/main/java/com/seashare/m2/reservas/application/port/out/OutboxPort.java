package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;

public interface OutboxPort { // puerto para encolar eventos en outbox

    void enqueue(StatusChangedEvent event); // encola un evento en la tabla outbox

    void enqueue(ReservationInfoProvidedEvent event); // encola evento de informacion de reserva para modulo 3
}
