package com.seashare.m2.reservas.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface NoShowEventPort { // puerto de salida para registrar el evento de inasistencia

    void save(UUID reservationId, UUID ownerId, Instant reportedAt, int waitingMinutes, String observations); // inserta una fila inmutable en noshow_event
}