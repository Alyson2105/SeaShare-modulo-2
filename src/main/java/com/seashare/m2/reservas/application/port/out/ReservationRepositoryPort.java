package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.ReservationPage;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReservationRepositoryPort { // puerto de persistencia para el agregado de reservas

    Reservation create(Reservation newReservation); // persiste formalmente la reserva

    Reservation save(Reservation reservation); // guarda la reserva retornando version actualizada

    Optional<Reservation> findById(UUID id); // recupera una reserva por identificador

    boolean existsOverlap(UUID vesselId, LocalDate start, LocalDate end, Set<ReservationStatus> blockingStatuses); // evalua solapamiento temporal con reservas bloqueantes

    ReservationPage findByRenter(UUID renterId, int page, int limit); // consulta reservas paginadas por arrendatario

    ReservationPage findByOwner(UUID ownerId, int page, int limit); // consulta reservas paginadas por propietario

    /**
     * Retorna los IDs de reservas en estado INITIATED o PENDING_PAYMENT cuyo ttl_expires_at sea menor o igual a {@code now},
     * ordenadas por ttl_expires_at ascendente y limitadas a {@code batchSize}.
     * Usa el índice ix_reservation_ttl existente. El corte es exacto: en el segundo de vencimiento ya se recupera.
     */
    List<UUID> findExpiredBefore(Instant now, int batchSize); // lectura de IDs vencidos (INITIATED o PENDING_PAYMENT) para el scheduler de TTL
}

