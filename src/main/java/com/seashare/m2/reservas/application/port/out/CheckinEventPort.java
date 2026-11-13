package com.seashare.m2.reservas.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface CheckinEventPort { // puerto de salida para registrar el evento de check-in

    void save(UUID reservationId, UUID ownerId, Instant actualTime, String notes); // inserta una fila inmutable en checkin_event
}