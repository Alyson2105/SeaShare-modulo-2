package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxPort { // puerto para encolar eventos en outbox

    void enqueue(StatusChangedEvent event); // encola un evento en la tabla outbox

    void enqueue(ReservationInfoProvidedEvent event); // encola evento de informacion de reserva para modulo 3

    List<PendingOutboxMessage> findPending(Instant now, int batchSize); // T-03: pendientes vencidos ordenados, hasta batchSize

    void markPublished(UUID messageId, Instant publishedAt); // T-03: marca published_at tras el confirm; purga payload con token (D-09)

    void markFailed(UUID messageId, int attempts, Instant nextAttemptAt, String lastError, boolean dead); // T-03: registra fallo, reintento o dead
}
