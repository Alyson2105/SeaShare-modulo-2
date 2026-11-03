package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageEntity, UUID> { // repositorio jpa para mensajes outbox
}
