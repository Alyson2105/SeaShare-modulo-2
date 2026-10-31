package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> { // repositorio jpa para la entidad reservation

    boolean existsByReservationCode(String code); // verifica si ya existe una reserva con el codigo provisto

    @Query("select count(r) > 0 from ReservationEntity r where r.vesselId = :vesselId and r.status in :statuses and r.startDate <= :endDate and r.endDate >= :startDate")
    boolean existsOverlapping(
            @Param("vesselId") UUID vesselId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    ); // comprueba solapamiento de fechas con estados bloqueantes

    Page<ReservationEntity> findByRenterId(UUID renterId, Pageable pageable); // consulta paginada por arrendatario

    Page<ReservationEntity> findByOwnerId(UUID ownerId, Pageable pageable); // consulta paginada por propietario

    boolean existsByIdAndRenterId(UUID id, UUID renterId); // verifica pertenencia a arrendatario

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId); // verifica pertenencia a propietario
}
