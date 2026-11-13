package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.noshow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface NoShowEventJpaRepository extends JpaRepository<NoShowEventEntity, UUID> { // repositorio jpa del evento de inasistencia
}