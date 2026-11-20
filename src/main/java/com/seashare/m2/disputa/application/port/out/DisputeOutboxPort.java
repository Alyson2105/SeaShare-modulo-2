package com.seashare.m2.disputa.application.port.out;

import com.seashare.m2.disputa.application.dto.DisputeUpdatedEvent;

public interface DisputeOutboxPort { // encolado del evento de disputa en el outbox transaccional

    void enqueue(DisputeUpdatedEvent event); // inserta la fila AMQP en la misma transaccion del cambio de estado
}
