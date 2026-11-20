package com.seashare.m2.disputa.infrastructure.adapter.out.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface DisputeOutboxJpaRepository extends JpaRepository<DisputeOutboxEntity, UUID> { // insercion en el outbox
}
