package com.seashare.m2.reservas.infrastructure.adapter.out.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageEntity, UUID> { // repositorio jpa para mensajes outbox

    @Query(value = """
            select * from outbox_message m
            where m.published_at is null
              and m.dead = false
              and m.next_attempt_at <= :now
            order by m.next_attempt_at asc, m.created_at asc
            limit :batchSize
            for update skip locked
            """, nativeQuery = true) // reclama el lote dentro de la transaccion que lo procesa
    List<OutboxMessageEntity> findPending(@Param("now") Instant now, @Param("batchSize") int batchSize); // filas bloqueadas hasta el commit
}
