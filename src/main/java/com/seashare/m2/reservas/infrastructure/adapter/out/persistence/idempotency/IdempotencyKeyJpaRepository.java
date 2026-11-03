package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyId> { // repositorio jpa para claves de idempotencia

    @Modifying
    @Query(value = "INSERT INTO idempotency_key (key, endpoint, response_ref, created_at) "
            + "VALUES (:key, :endpoint, NULL, :now) "
            + "ON CONFLICT (key, endpoint) DO UPDATE "
            + "SET response_ref = NULL, created_at = EXCLUDED.created_at "
            + "WHERE idempotency_key.created_at <= :threshold", nativeQuery = true)
    int tryClaim(@Param("key") String key,
                 @Param("endpoint") String endpoint,
                 @Param("now") OffsetDateTime now,
                 @Param("threshold") OffsetDateTime threshold); // upsert atomico condicionado a la ventana de expiracion

    @Modifying(clearAutomatically = true)
    @Query("UPDATE IdempotencyKeyEntity e SET e.responseRef = :ref "
            + "WHERE e.id.idempotencyKey = :key AND e.id.endpoint = :endpoint")
    int complete(@Param("ref") String ref,
                 @Param("key") String key,
                 @Param("endpoint") String endpoint); // asocia la referencia del resultado a la clave reclamada

    @Query("SELECT e.responseRef FROM IdempotencyKeyEntity e "
            + "WHERE e.id.idempotencyKey = :key AND e.id.endpoint = :endpoint")
    Optional<String> findResponseRef(@Param("key") String key,
                                     @Param("endpoint") String endpoint); // recupera la referencia del resultado ya persistida
}
