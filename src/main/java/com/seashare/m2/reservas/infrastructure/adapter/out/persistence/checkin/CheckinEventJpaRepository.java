package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CheckinEventJpaRepository extends JpaRepository<CheckinEventEntity, UUID> { // repositorio jpa del evento de check-in
}