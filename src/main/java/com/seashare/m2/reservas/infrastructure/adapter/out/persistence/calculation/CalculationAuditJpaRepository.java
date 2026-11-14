package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.calculation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CalculationAuditJpaRepository extends JpaRepository<CalculationAuditEntity, UUID> { // repositorio jpa de auditoria de calculo

    List<CalculationAuditEntity> findByReservationIdOrderByOccurredAtDesc(UUID reservationId); // asientos de una reserva
}
