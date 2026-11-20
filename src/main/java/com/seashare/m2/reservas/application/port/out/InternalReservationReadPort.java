package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.InternalReservationView;

import java.util.Optional;
import java.util.UUID;

public interface InternalReservationReadPort { // puerto de lectura dedicado al endpoint interno de Modulo 3

    Optional<InternalReservationView> findById(UUID reservationId); // busca la reserva sin escribir ni emitir eventos
}
