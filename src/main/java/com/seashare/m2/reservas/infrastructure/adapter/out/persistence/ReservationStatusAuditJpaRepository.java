package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationStatusAuditJpaRepository extends JpaRepository<ReservationStatusAuditEntity, UUID> { // repositorio jpa para auditoria de estados
}
