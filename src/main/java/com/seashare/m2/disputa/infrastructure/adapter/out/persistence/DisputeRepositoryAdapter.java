package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DisputeRepositoryAdapter implements DisputeRepositoryPort { // adaptador jpa del puerto de disputas

    private final DisputeJpaRepository disputes; // repositorio de disputas

    private final DisputeClaimJpaRepository claims; // repositorio de reclamos

    private final DisputeStatusAuditJpaRepository audit; // repositorio del historial

    public DisputeRepositoryAdapter(DisputeJpaRepository disputes,
                                    DisputeClaimJpaRepository claims,
                                    DisputeStatusAuditJpaRepository audit) {
        this.disputes = disputes;
        this.claims = claims;
        this.audit = audit;
    }

    @Override
    public Optional<GuaranteeDispute> findById(UUID disputeId) {
        return disputes.findById(disputeId).map(DisputeMapper::toDomain);
    }

    @Override
    public Optional<GuaranteeDispute> findByReservationId(UUID reservationId) {
        return disputes.findByReservationId(reservationId).map(DisputeMapper::toDomain);
    }

    @Override
    public Optional<DisputeClaim> findClaimByDisputeId(UUID disputeId) {
        return claims.findByDisputeId(disputeId).map(DisputeMapper::toDomain);
    }

    @Override
    public List<DisputeStatusChange> findStatusHistory(UUID disputeId) {
        return audit.findByDisputeIdOrderByOccurredAtAsc(disputeId).stream()
                .map(DisputeMapper::toDomain)
                .toList();
    }

    @Override
    public GuaranteeDispute save(GuaranteeDispute dispute) {
        DisputeEntity entity = disputes.findById(dispute.id()).orElseGet(DisputeEntity::new);
        DisputeMapper.copyToEntity(dispute, entity); // actualiza solo el estado y la resolucion
        return DisputeMapper.toDomain(disputes.save(entity));
    }

    @Override
    public DisputeClaim saveClaim(DisputeClaim claim) {
        return DisputeMapper.toDomain(claims.save(DisputeMapper.toEntity(claim)));
    }

    @Override
    public void recordStatusChange(DisputeStatusChange change) {
        audit.save(DisputeMapper.toEntity(change));
    }
}
