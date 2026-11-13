package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.checkout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CheckoutEventJpaRepository extends JpaRepository<CheckoutEventEntity, UUID> { // repositorio jpa del evento de checkout
}