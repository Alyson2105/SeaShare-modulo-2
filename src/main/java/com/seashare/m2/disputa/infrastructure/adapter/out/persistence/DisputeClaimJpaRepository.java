package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface DisputeClaimJpaRepository extends JpaRepository<DisputeClaimEntity, UUID> { // repositorio spring data de reclamos

    Optional<DisputeClaimEntity> findByDisputeId(UUID disputeId); // un unico reclamo por disputa (D-08)
}
