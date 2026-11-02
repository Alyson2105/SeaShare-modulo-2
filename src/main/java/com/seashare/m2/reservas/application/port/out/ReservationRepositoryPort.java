package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.domain.model.NewReservation;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReservationRepositoryPort { // puerto de persistencia para el agregado de reservas

    Optional<Reservation> findById(UUID id); // recupera una reserva por identificador

    Reservation save(Reservation reservation); // guarda la reserva retornando version actualizada

    boolean existsOverlap(UUID vesselId, LocalDate startDate, LocalDate endDate, Set<ReservationStatus> blockingStatuses); // evalua solapamiento temporal con reservas bloqueantes

    ReservationView create(NewReservation reservation); // persiste formalmente la reserva y retorna su vista

    Optional<ReservationView> findViewById(UUID reservationId); // busca una reserva por su identificador unico

    record ReservationPage( // resultado inmutable de consulta paginada
            java.util.List<ReservationView> items, // lista de vistas de reservas en la pagina actual
            long totalItems // cantidad total de registros coincidentes
    ) {
    }

    ReservationPage findByRenter(UUID renterId, int page, int limit); // consulta reservas paginadas por arrendatario

    ReservationPage findByOwner(UUID ownerId, int page, int limit); // consulta reservas paginadas por propietario

    Optional<ReservationView> findByIdView(UUID reservationId); // busca reserva por su identificador unico

    boolean isOwnedByUser(UUID reservationId, UUID userId, String role); // valida pertenencia de la reserva segun rol
}
