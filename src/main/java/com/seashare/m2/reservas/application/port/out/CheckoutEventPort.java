package com.seashare.m2.reservas.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface CheckoutEventPort { // puerto de salida para registrar el evento de checkout

    void save(UUID reservationId, UUID ownerId, Instant actualTime, String remarks); // inserta una fila inmutable en checkout_event
}