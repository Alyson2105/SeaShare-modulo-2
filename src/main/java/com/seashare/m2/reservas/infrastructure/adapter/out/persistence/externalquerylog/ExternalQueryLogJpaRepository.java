package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.externalquerylog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExternalQueryLogJpaRepository extends JpaRepository<ExternalQueryLogEntity, UUID> { // repositorio jpa de consultas a M1
}
