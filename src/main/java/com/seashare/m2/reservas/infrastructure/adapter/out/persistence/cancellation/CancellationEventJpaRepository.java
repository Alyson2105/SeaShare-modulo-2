package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.cancellation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CancellationEventJpaRepository extends JpaRepository<CancellationEventEntity, UUID> { // repositorio jpa del evento de cancelacion
}