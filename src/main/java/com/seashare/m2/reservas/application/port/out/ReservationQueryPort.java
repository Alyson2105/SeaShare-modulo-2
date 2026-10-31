package com.seashare.m2.reservas.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationQueryPort { // puerto de salida para consultas paginadas y detalle de reservas

    record ReservationPage( // resultado inmutable de consulta paginada
            List<ReservationView> items, // lista de vistas de reservas en la pagina actual
            long totalItems // cantidad total de registros coincidentes
    ) {
    }

    ReservationPage findByRenter(UUID renterId, int page, int limit); // consulta reservas paginadas por arrendatario

    ReservationPage findByOwner(UUID ownerId, int page, int limit); // consulta reservas paginadas por propietario

    Optional<ReservationView> findById(UUID reservationId); // busca reserva por su identificador unico

    boolean isOwnedByUser(UUID reservationId, UUID userId, String role); // valida pertenencia de la reserva segun rol
}
