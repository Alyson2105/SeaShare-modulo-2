package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageEntity, UUID> { // repositorio jpa para mensajes outbox

    @Query("""
            select m from OutboxMessageEntity m
            where m.publishedAt is null
              and m.dead = false
              and m.nextAttemptAt <= :now
            order by m.nextAttemptAt asc, m.createdAt asc
            """) // pendientes del relay: sin publicar, vivos y con el proximo intento vencido
    List<OutboxMessageEntity> findPending(@Param("now") Instant now, Pageable pageable); // lote ordenado por vencimiento
}
