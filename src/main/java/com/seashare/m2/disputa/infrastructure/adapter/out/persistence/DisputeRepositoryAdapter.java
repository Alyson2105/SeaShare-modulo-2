package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.model.AdminReview;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import com.seashare.m2.disputa.domain.model.DisputeCategory;
import java.sql.Timestamp;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DisputeRepositoryAdapter implements DisputeRepositoryPort { // adaptador jpa del puerto de disputas

    private final DisputeJpaRepository disputes; // repositorio de disputas

    private final DisputeClaimJpaRepository claims; // repositorio de reclamos

    private final DisputeStatusAuditJpaRepository audit; // repositorio del historial

    private final AdminReviewJpaRepository reviews; // repositorio de revisiones administrativas
    private final JdbcTemplate jdbc;
    private final JsonMapper json = new JsonMapper();

    public DisputeRepositoryAdapter(DisputeJpaRepository disputes,
                                    DisputeClaimJpaRepository claims,
                                    DisputeStatusAuditJpaRepository audit,
                                    AdminReviewJpaRepository reviews, JdbcTemplate jdbc) {
        this.disputes = disputes;
        this.claims = claims;
        this.audit = audit;
        this.reviews = reviews;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<GuaranteeDispute> findById(UUID disputeId) {
        return disputes.findById(disputeId).map(DisputeMapper::toDomain);
    }

    @Override
    public Optional<GuaranteeDispute> findByIdForUpdate(UUID disputeId) {
        return disputes.findByIdForUpdate(disputeId).map(DisputeMapper::toDomain);
    }

    @Override
    public Optional<GuaranteeDispute> findByReservationId(UUID reservationId) {
        return disputes.findByReservationId(reservationId).map(DisputeMapper::toDomain);
    }

    @Override
    public List<GuaranteeDispute> findAll(UUID ownerId, UUID reservationId) {
        return disputes.findAll(ownerId, reservationId).stream()
                .map(DisputeMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DisputeClaim> findClaimByDisputeId(UUID disputeId) {
        return claims.findByDisputeId(disputeId).map(entity -> {
            DisputeClaim original = DisputeMapper.toDomain(entity);
            return jdbc.query("""
                    SELECT description, category, evidence_urls FROM dispute_claim_revision
                    WHERE claim_id = ? ORDER BY revision_id DESC LIMIT 1
                    """, rs -> rs.next() ? new DisputeClaim(original.id(), original.disputeId(), original.ownerId(),
                    rs.getString("description"), DisputeCategory.fromStored(rs.getString("category")),
                    List.of(json.readValue(rs.getString("evidence_urls"), String[].class)), original.registeredAt()) : original,
                    original.id());
        });
    }

    @Override
    public List<DisputeStatusChange> findStatusHistory(UUID disputeId) {
        return audit.findByDisputeIdOrderByOccurredAtAsc(disputeId).stream()
                .map(DisputeMapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findPendingDue(Instant now, int limit) {
        return disputes.findPendingDue(now, limit);
    }

    @Override
    public List<AdminReview> findAdminReviews(UUID disputeId) {
        return reviews.findByDisputeIdOrderByReviewedAtAsc(disputeId).stream()
                .map(DisputeMapper::toDomain)
                .toList();
    }

    @Override
    public GuaranteeDispute save(GuaranteeDispute dispute) {
        DisputeEntity entity = disputes.findById(dispute.id()).orElseGet(DisputeEntity::new);
        DisputeMapper.copyToEntity(dispute, entity); // actualiza solo el estado y la resolucion
        return DisputeMapper.toDomain(disputes.saveAndFlush(entity)); // flush: el conflicto de version se ve aqui
    }

    @Override
    public DisputeClaim saveClaim(DisputeClaim claim) {
        return DisputeMapper.toDomain(claims.save(DisputeMapper.toEntity(claim)));
    }

    @Override
    public DisputeClaim saveClaimRevision(DisputeClaim claim, Instant editedAt) {
        jdbc.update("""
                INSERT INTO dispute_claim_revision (claim_id, owner_id, description, category, evidence_urls, edited_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, claim.id(), claim.ownerId(), claim.description(),
                claim.category() == null ? null : claim.category().wireValue(),
                json.writeValueAsString(claim.evidenceUrls()), Timestamp.from(editedAt));
        return claim;
    }

    @Override
    public void saveAdminReview(AdminReview review) {
        reviews.save(DisputeMapper.toEntity(review));
    }

    @Override
    public void recordStatusChange(DisputeStatusChange change) {
        audit.save(DisputeMapper.toEntity(change));
    }
}
