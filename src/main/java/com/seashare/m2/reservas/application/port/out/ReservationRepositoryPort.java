package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.domain.model.Reservation;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepositoryPort { // puerto de persistencia para el agregado de reservas

    Optional<Reservation> findById(UUID id); // recupera una reserva por identificador

    Reservation save(Reservation reservation); // guarda la reserva retornando version actualizada
}
