package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DisputeJpaRepository extends JpaRepository<DisputeEntity, UUID> { // repositorio spring data de disputas

    Optional<DisputeEntity> findByReservationId(UUID reservationId); // una disputa por reserva (UNIQUE en V2)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DisputeEntity d where d.id = :id")
    Optional<DisputeEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            select d.id from dispute_guarantee d
            where d.status = 'PENDING'
              and d.has_claim = false
              and d.window_end <= :now
            order by d.window_end asc
            limit :limit
            """, nativeQuery = true) // barrido del cierre automatico (indice ix_dispute_due)
    List<UUID> findPendingDue(@Param("now") Instant now, @Param("limit") int limit);
}
