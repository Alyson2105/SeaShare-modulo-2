package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AdminReviewJpaRepository extends JpaRepository<AdminReviewEntity, UUID> { // revisiones administrativas

    List<AdminReviewEntity> findByDisputeIdOrderByReviewedAtAsc(UUID disputeId); // historial de revision
}
