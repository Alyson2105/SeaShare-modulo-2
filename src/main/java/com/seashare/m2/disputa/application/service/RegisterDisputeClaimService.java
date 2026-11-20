package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.in.RegisterDisputeClaimUseCase;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeCategory;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Registro del reclamo del propietario (CU-16, T-18a).
 *
 * <p>Reglas: el estado sigue PENDING, se admite un solo reclamo (D-08), la
 * ventana debe estar abierta y el reclamante debe ser el propietario de la
 * reserva. No hay montos ni operaciones financieras (FR-010).</p>
 */
@Service
@Transactional
public class RegisterDisputeClaimService implements RegisterDisputeClaimUseCase {

    private final DisputeRepositoryPort repository; // persistencia de disputas y reclamos

    private final ClockPort clockPort; // unico reloj de la aplicacion

    public RegisterDisputeClaimService(DisputeRepositoryPort repository, ClockPort clockPort) {
        this.repository = repository;
        this.clockPort = clockPort;
    }

    @Override
    public DisputeClaimView register(RegisterDisputeClaimCommand command) {
        GuaranteeDispute dispute = repository.findById(command.disputeId())
                .orElseThrow(() -> new DisputeNotFoundException(command.disputeId())); // 404 DISPUTE_NOT_FOUND

        if (!dispute.ownerId().equals(command.claimantId())) { // solo el propietario registrado (CU-16 FR-006)
            throw new DisputeRuleException(ErrorCode.RESERVATION_NOT_OWNED,
                    "Only the registered owner of the reserved vessel may file the claim");
        }

        Instant now = clockPort.now(); // hora unica para estado y ventana
        if (dispute.isFinal()) { // los estados finales no admiten reclamo (CU-16 edge cases)
            if (dispute.status() == DisputeStatus.REJECTED && !dispute.hasClaim()) { // cerrada por vencimiento
                throw windowExpired();
            }
            throw new DisputeRuleException(ErrorCode.DISPUTE_ALREADY_RESOLVED,
                    "The dispute has already reached a final state");
        }
        if (dispute.hasClaim()) { // D-08: un unico reclamo, no editable
            throw new DisputeRuleException(ErrorCode.CLAIM_ALREADY_EXISTS,
                    "A claim has already been registered for this dispute");
        }
        if (!dispute.canRegisterClaim(now)) { // ventana cerrada: ahora >= window_end
            throw windowExpired();
        }

        DisputeClaim claim = DisputeClaim.register(
                dispute.id(), command.claimantId(), command.description(),
                DisputeCategory.fromWire(command.damageCategory()), command.evidenceUrls(), now);
        repository.saveClaim(claim); // solo INSERT: la tabla es inmutable

        dispute.registerClaim(); // el estado sigue PENDING (FR-006)
        GuaranteeDispute saved = repository.save(dispute);

        return DisputeClaimView.of(saved, claim);
    }

    private DisputeRuleException windowExpired() { // 409 WINDOW_EXPIRED sin modificar el estado (FR-009)
        return new DisputeRuleException(ErrorCode.WINDOW_EXPIRED,
                "The 24 hour claim window has expired");
    }
}
