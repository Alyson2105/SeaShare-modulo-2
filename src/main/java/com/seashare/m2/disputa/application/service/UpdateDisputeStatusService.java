package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusResult;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
import com.seashare.m2.disputa.application.port.out.DisputeOutboxPort;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.AdminReview;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Unica operacion de cambio de estado de la disputa (CU-17, T-18b).
 *
 * <p>La usan el Admin y el cierre automatico por vencimiento. Escribe, en la
 * misma transaccion: la disputa, el historial de estados y (solo para el Admin)
 * la revision administrativa con su motivo local. No hay montos ni pasarela.</p>
 */
@Service
@Transactional
public class UpdateDisputeStatusService implements UpdateDisputeStatusUseCase {

    private final DisputeRepositoryPort repository; // persistencia de disputas

    private final DisputeOutboxPort disputeOutboxPort; // encolado del evento final (CU-18)

    private final ClockPort clockPort; // unico reloj de la aplicacion

    public UpdateDisputeStatusService(DisputeRepositoryPort repository,
                                      DisputeOutboxPort disputeOutboxPort,
                                      ClockPort clockPort) {
        this.repository = repository;
        this.disputeOutboxPort = disputeOutboxPort;
        this.clockPort = clockPort;
    }

    @Override
    public UpdateDisputeStatusResult update(UpdateDisputeStatusCommand command) {
        if (command == null || command.actorType() == null) { // comando obligatorio
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS, "UpdateDisputeStatusCommand is required");
        }
        GuaranteeDispute dispute = repository.findById(command.disputeId())
                .orElseThrow(() -> new DisputeNotFoundException(command.disputeId())); // 404
        DisputeStatus previous = dispute.status(); // estado previo para la respuesta y el historial
        Instant now = clockPort.now(); // hora unica de la transicion

        dispute.resolve(command.targetStatus(), command.adminId(), command.reason(), now); // valida y transiciona

        if (command.actorType() == DisputeActorType.ADMIN) { // trazabilidad de la revision humana
            repository.saveAdminReview(AdminReview.record(dispute.id(), command.adminId(),
                    dispute.status(), command.reason(), command.internalNotes(), now));
        }

        if (previous != dispute.status()) { // solo hay transicion efectiva si el estado cambia
            try {
                repository.save(dispute); // flush inmediato para traducir el conflicto de version
            } catch (OptimisticLockingFailureException e) { // carrera Admin vs cierre automatico
                throw new DisputeRuleException(ErrorCode.DISPUTE_ALREADY_RESOLVED,
                        "The dispute was resolved concurrently");
            }
            repository.recordStatusChange(DisputeStatusChange.record(dispute.id(), previous, dispute.status(),
                    command.actorType(), command.adminId(), dispute.reason(), now));

            if (dispute.status().isFinal()) { // CU-18: solo los estados finales generan evento
                disputeOutboxPort.enqueue(DisputeEventMapper.finalEvent(dispute, now)); // misma transaccion
            }
        }

        return new UpdateDisputeStatusResult(
                dispute.id(),
                dispute.reservationId(),
                previous,
                dispute.status(),
                dispute.resolvedBy(),
                dispute.resolvedAt(),
                dispute.reason(),
                dispute.status().isFinal()); // T-18c materializa el encolado en outbox
    }
}
