package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeListView;
import com.seashare.m2.disputa.application.port.in.ListDisputesUseCase;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListDisputesService implements ListDisputesUseCase {

    private final DisputeRepositoryPort repository;

    public ListDisputesService(DisputeRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<DisputeListView> list(UUID userId, String role, UUID reservationId) {
        UUID ownerId = null;
        if ("OWNER".equalsIgnoreCase(role) && userId != null) {
            ownerId = userId;
        } else if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new DisputeRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only ADMIN or OWNER roles are authorized to list disputes");
        }

        return repository.findAll(ownerId, reservationId).stream()
                .map(dispute -> new DisputeListView(
                        dispute.id(),
                        dispute.reservationId(),
                        dispute.status(),
                        dispute.window().start(),
                        dispute.window().end(),
                        repository.findClaimByDisputeId(dispute.id()).orElse(null)))
                .toList();
    }
}
