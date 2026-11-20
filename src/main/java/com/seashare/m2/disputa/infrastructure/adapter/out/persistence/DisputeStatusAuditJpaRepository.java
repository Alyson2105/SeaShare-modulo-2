package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DisputeStatusAuditJpaRepository extends JpaRepository<DisputeStatusAuditEntity, UUID> { // historial inmutable

    List<DisputeStatusAuditEntity> findByDisputeIdOrderByOccurredAtAsc(UUID disputeId); // orden cronologico
}
