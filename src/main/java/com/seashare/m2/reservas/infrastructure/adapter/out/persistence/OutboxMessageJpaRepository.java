package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageEntity, UUID> { // repositorio jpa para mensajes outbox
}
