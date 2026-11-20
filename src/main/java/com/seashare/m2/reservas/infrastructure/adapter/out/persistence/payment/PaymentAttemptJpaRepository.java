package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    boolean existsByReservationIdAndExternalReferenceAndResult(UUID reservationId,
                                                              String externalReference,
                                                              String result); // idempotencia estricta (FR-007)

    Optional<PaymentAttemptEntity> findTopByReservationIdOrderByConsultedAtDesc(UUID reservationId); // ultimo intento

    long countByReservationId(UUID reservationId); // numero de intentos
}
