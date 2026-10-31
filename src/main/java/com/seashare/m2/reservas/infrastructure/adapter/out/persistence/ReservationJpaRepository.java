package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> { // repositorio jpa para la entidad reservation
}
