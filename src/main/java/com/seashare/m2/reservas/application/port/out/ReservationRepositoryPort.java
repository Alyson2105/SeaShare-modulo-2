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

    Optional<Reservation> findByIdForUpdate(UUID id); // CU-03: recupera y bloquea la fila hasta el commit

    boolean existsOverlap(UUID vesselId, LocalDate start, LocalDate end, Set<ReservationStatus> blockingStatuses); // evalua solapamiento temporal con reservas bloqueantes

    ReservationPage findByRenter(UUID renterId, int page, int limit); // consulta reservas paginadas por arrendatario

    ReservationPage findByOwner(UUID ownerId, int page, int limit); // consulta reservas paginadas por propietario

    /**
     * Consulta paginada por arrendatario restringida a los estados indicados.
     * Un conjunto nulo o vacio equivale a no filtrar por estado.
     */
    ReservationPage findByRenter(UUID renterId, int page, int limit, Set<ReservationStatus> statuses); // consulta filtrada del arrendatario

    /**
     * Consulta paginada por propietario restringida a los estados indicados.
     * Un conjunto nulo o vacio equivale a no filtrar por estado.
     */
    ReservationPage findByOwner(UUID ownerId, int page, int limit, Set<ReservationStatus> statuses); // consulta filtrada del propietario

    /**
     * Retorna los IDs de reservas en estado INITIATED o PENDING_PAYMENT cuyo ttl_expires_at sea menor o igual a {@code now},
     * ordenadas por ttl_expires_at ascendente y limitadas a {@code batchSize}.
     * Usa el índice ix_reservation_ttl existente. El corte es exacto: en el segundo de vencimiento ya se recupera.
     */
    List<UUID> findExpiredBefore(Instant now, int batchSize); // lectura de IDs vencidos (INITIATED o PENDING_PAYMENT) para el scheduler de TTL

    /**
     * Candidatos del job de confirmacion de cobro (CU-13): reservas en PENDING_PAYMENT
     * y reservas EXPIRED o PAYMENT_FAILED recientes, para detectar cobros huerfanos.
     */
    List<UUID> findPaymentCandidates(Instant since, int batchSize); // reservas a consultar en modulo 3

    /**
     * Cuenta las reservas COMPLETED del arrendatario, excluyendo la reserva que se
     * esta consultando. Es un conteo de calendario, no un calculo monetario (CU-21 FR-009).
     */
    long countCompletedByRenterExcluding(UUID renterId, UUID excludeReservationId); // viajes completados registrados en M2

    /**
     * Indica si existe otra reserva de la misma embarcacion en estado IN_NAVIGATION,
     * excluyendo la reserva que se esta evaluando (CU-06).
     */
    boolean existsOtherInNavigation(UUID vesselId, UUID excludeReservationId); // otra travesia activa del mismo barco
}

