package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.payment;

import com.seashare.m2.reservas.application.port.out.PaymentAttemptPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentAttemptAdapter implements PaymentAttemptPort { // persistencia del historial de cobros

    private final PaymentAttemptJpaRepository repository;

    public PaymentAttemptAdapter(PaymentAttemptJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean registerIfAbsent(UUID reservationId,
                                    String externalReference,
                                    String result,
                                    String detail,
                                    Instant consultedAt) { // devuelve false si la clave ya existia
        if (repository.existsByReservationIdAndExternalReferenceAndResult(reservationId, externalReference, result)) {
            return false; // notificacion duplicada: no se repiten efectos
        }
        PaymentAttemptEntity entity = new PaymentAttemptEntity();
        entity.setReservationId(reservationId);
        entity.setExternalReference(externalReference);
        entity.setResult(result);
        entity.setDetail(detail);
        entity.setConsultedAt(consultedAt);
        try {
            repository.saveAndFlush(entity); // fuerza el indice unico
            return true; // primera vez que se procesa esta clave
        } catch (DataIntegrityViolationException e) { // carrera entre dos canales o ciclos
            return false; // otro proceso gano la clave
        }
    }

    @Override
    public int countAttempts(UUID reservationId) {
        return (int) repository.countByReservationId(reservationId);
    }

    @Override
    public Optional<String> lastResult(UUID reservationId) {
        return repository.findTopByReservationIdOrderByConsultedAtDesc(reservationId)
                .map(PaymentAttemptEntity::getResult);
    }
}
