package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeCommand;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeUseCase;
import com.seashare.m2.shared.contracts.dispute.OpenedDispute;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Apertura automatica de la disputa de garantia (CU-16, T-18a).
 *
 * <p>Implementa el contrato de {@code shared/contracts/dispute} que consume CU-07.
 * Es transaccional (REQUIRED) para crear la disputa en la misma transaccion del
 * paso a COMPLETED, e idempotente por reserva (D-10): si ya existe, devuelve la
 * existente aunque el comando traiga datos distintos.</p>
 */
@Service
@Transactional
public class OpenDisputeService implements OpenDisputeUseCase {

    static final String CREATION_REASON = "Apertura automatica al completar la reserva (CU-07)"; // motivo del historial

    private final DisputeRepositoryPort repository; // persistencia de disputas

    public OpenDisputeService(DisputeRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public OpenedDispute open(OpenDisputeCommand command) {
        if (command == null) { // contrato interno: el comando es obligatorio
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS, "OpenDisputeCommand is required");
        }
        Optional<GuaranteeDispute> existing = repository.findByReservationId(command.reservationId()); // idempotencia
        if (existing.isPresent()) { // una reserva tiene una sola disputa (V2 UNIQUE reservation_id)
            return toOpened(existing.get());
        }

        GuaranteeDispute dispute = GuaranteeDispute.open(
                command.reservationId(), command.ownerId(), command.completedAt()); // nace PENDING con ventana de 24 h
        GuaranteeDispute saved = repository.save(dispute);
        repository.recordStatusChange(DisputeStatusChange.record(
                saved.id(), null, saved.status(), DisputeActorType.SYSTEM, null,
                CREATION_REASON, command.completedAt())); // historial: no habia estado anterior

        return toOpened(saved);
    }

    private OpenedDispute toOpened(GuaranteeDispute dispute) { // proyecta el agregado al contrato compartido
        return new OpenedDispute(
                dispute.id(),
                dispute.reservationId(),
                dispute.status().name(),
                dispute.window().start(),
                dispute.window().end());
    }
}
