package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReservationCreationPort { // puerto de persistencia para creacion y consulta de reservas

    boolean existsOverlap(UUID vesselId, LocalDate startDate, LocalDate endDate, Set<ReservationStatus> blockingStatuses); // evalua solapamiento temporal con reservas bloqueantes

    ReservationView create(NewReservation reservation); // persiste formalmente la reserva y retorna su vista

    Optional<ReservationView> findViewById(UUID reservationId); // busca una reserva por su identificador unico
}
