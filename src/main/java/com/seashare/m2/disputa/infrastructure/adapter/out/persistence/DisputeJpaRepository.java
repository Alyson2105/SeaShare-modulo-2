package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface DisputeJpaRepository extends JpaRepository<DisputeEntity, UUID> { // repositorio spring data de disputas

    Optional<DisputeEntity> findByReservationId(UUID reservationId); // una disputa por reserva (UNIQUE en V2)
}
