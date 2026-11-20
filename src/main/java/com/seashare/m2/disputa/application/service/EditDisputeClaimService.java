package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.in.EditDisputeClaimUseCase;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeCategory;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditDisputeClaimService implements EditDisputeClaimUseCase {
    private final DisputeRepositoryPort repository;
    private final ClockPort clock;

    public EditDisputeClaimService(DisputeRepositoryPort repository, ClockPort clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DisputeClaimView edit(RegisterDisputeClaimCommand command) {
        var dispute = repository.findByIdForUpdate(command.disputeId())
                .orElseThrow(() -> new DisputeNotFoundException(command.disputeId()));
        if (!dispute.ownerId().equals(command.claimantId())) {
            throw new DisputeRuleException(ErrorCode.RESERVATION_NOT_OWNED, "Only the owner can edit the claim");
        }
        if (dispute.isFinal()) {
            throw new DisputeRuleException(ErrorCode.DISPUTE_ALREADY_RESOLVED, "The dispute is already resolved");
        }
        var now = clock.now();
        if (dispute.isExpired(now)) {
            throw new DisputeRuleException(ErrorCode.WINDOW_EXPIRED, "The claim window has expired");
        }
        var previous = repository.findClaimByDisputeId(dispute.id())
                .orElseThrow(() -> new DisputeRuleException(ErrorCode.INVALID_RESERVATION_STATE, "No claim to edit"));
        var updated = new DisputeClaim(previous.id(), previous.disputeId(), previous.ownerId(),
                command.description(), DisputeCategory.fromWire(command.damageCategory()),
                command.evidenceUrls(), previous.registeredAt());
        return DisputeClaimView.of(dispute, repository.saveClaimRevision(updated, now));
    }
}
