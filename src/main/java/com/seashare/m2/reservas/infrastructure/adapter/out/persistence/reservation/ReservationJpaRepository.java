package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.reservation;

import com.seashare.m2.reservas.domain.model.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> { // repositorio jpa para la entidad reservation

    boolean existsByReservationCode(String code); // verifica si ya existe una reserva con el codigo provisto

    @Lock(LockModeType.PESSIMISTIC_WRITE) // CU-03 FR-005: bloqueo pesimista de la fila
    @Query("select r from ReservationEntity r where r.id = :id")
    Optional<ReservationEntity> findByIdForUpdate(@Param("id") UUID id); // reserva bloqueada hasta el commit

    @Query("select count(r) > 0 from ReservationEntity r where r.vesselId = :vesselId and r.status in :statuses and r.startDate <= :endDate and r.endDate >= :startDate")
    boolean existsOverlapping(
            @Param("vesselId") UUID vesselId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    ); // comprueba solapamiento de fechas con estados bloqueantes

    Page<ReservationEntity> findByRenterId(UUID renterId, Pageable pageable); // consulta paginada por arrendatario

    Page<ReservationEntity> findByOwnerId(UUID ownerId, Pageable pageable); // consulta paginada por propietario

    Page<ReservationEntity> findByRenterIdAndStatusIn(UUID renterId, Collection<ReservationStatus> statuses, Pageable pageable); // consulta filtrada por arrendatario

    Page<ReservationEntity> findByOwnerIdAndStatusIn(UUID ownerId, Collection<ReservationStatus> statuses, Pageable pageable); // consulta filtrada por propietario

    boolean existsByIdAndRenterId(UUID id, UUID renterId); // verifica pertenencia a arrendatario

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId); // verifica pertenencia a propietario

    @Query("select r.id from ReservationEntity r where r.status in ('INITIATED', 'PENDING_PAYMENT') and r.ttlExpiresAt <= :now order by r.ttlExpiresAt asc")
    List<UUID> findExpiredBefore(@Param("now") Instant now, Pageable pageable); // busca IDs vencidos de ambos estados usando ix_reservation_ttl

    @Query("""
            select r.id from ReservationEntity r
            where r.status = 'PENDING_PAYMENT'
               or (r.status in ('EXPIRED', 'PAYMENT_FAILED') and r.statusChangedAt >= :since)
            order by r.ttlExpiresAt asc
            """)
    List<UUID> findPaymentCandidates(@Param("since") Instant since, Pageable pageable); // CU-13: cobros por consultar

    long countByRenterIdAndStatusAndIdNot(UUID renterId, ReservationStatus status, UUID id); // viajes completados del arrendatario excluyendo la reserva actual

    boolean existsByVesselIdAndStatusAndIdNot(UUID vesselId, ReservationStatus status, UUID id); // CU-06: otra reserva del barco en el estado indicado
}

