package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ReservationStatusAuditJpaRepository extends JpaRepository<ReservationStatusAuditEntity, UUID> { // repositorio jpa para auditoria de estados
}
