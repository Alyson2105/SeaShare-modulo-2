package com.seashare.m2.disputa.infrastructure.adapter.out.persistence;

import com.seashare.m2.disputa.domain.model.AdminReview;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeCategory;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

final class DisputeMapper { // traduccion entre el dominio de disputa y las entidades jpa

    private static final JsonMapper JSON = new JsonMapper(); // serializa evidence_urls como arreglo json

    private DisputeMapper() { // clase de utilidades
    }

    static void copyToEntity(GuaranteeDispute dispute, DisputeEntity entity) { // copia el agregado sobre la entidad
        entity.setId(dispute.id());
        entity.setReservationId(dispute.reservationId());
        entity.setOwnerId(dispute.ownerId());
        entity.setStatus(dispute.status());
        entity.setWindowStart(dispute.window().start());
        entity.setWindowEnd(dispute.window().end());
        entity.setHasClaim(dispute.hasClaim());
        entity.setReason(dispute.reason());
        entity.setResolvedBy(dispute.resolvedBy());
        entity.setResolvedAt(dispute.resolvedAt());
    }

    static GuaranteeDispute toDomain(DisputeEntity entity) { // reconstruye el agregado desde la fila
        return GuaranteeDispute.restore(
                entity.getId(),
                entity.getReservationId(),
                entity.getOwnerId(),
                entity.getWindowStart(),
                entity.getWindowEnd(),
                entity.getStatus(),
                entity.isHasClaim(),
                entity.getReason(),
                entity.getResolvedBy(),
                entity.getResolvedAt());
    }

    static DisputeClaimEntity toEntity(DisputeClaim claim) { // mapea el reclamo para insertarlo
        DisputeClaimEntity entity = new DisputeClaimEntity();
        entity.setId(claim.id());
        entity.setDisputeId(claim.disputeId());
        entity.setOwnerId(claim.ownerId());
        entity.setDescription(claim.description());
        entity.setCategory(claim.category() == null ? null : claim.category().wireValue());
        entity.setEvidenceUrls(toJson(claim.evidenceUrls()));
        entity.setRegisteredAt(claim.registeredAt());
        return entity;
    }

    static DisputeClaim toDomain(DisputeClaimEntity entity) { // reconstruye el reclamo desde la fila
        return new DisputeClaim(
                entity.getId(),
                entity.getDisputeId(),
                entity.getOwnerId(),
                entity.getDescription(),
                DisputeCategory.fromStored(entity.getCategory()),
                fromJson(entity.getEvidenceUrls()),
                entity.getRegisteredAt());
    }

    static DisputeStatusAuditEntity toEntity(DisputeStatusChange change) { // mapea una fila del historial
        DisputeStatusAuditEntity entity = new DisputeStatusAuditEntity();
        entity.setId(change.id());
        entity.setDisputeId(change.disputeId());
        entity.setFromStatus(change.fromStatus() == null ? null : change.fromStatus().name());
        entity.setToStatus(change.toStatus().name());
        entity.setActorType(change.actorType().name());
        entity.setActorId(change.actorId());
        entity.setReason(change.reason());
        entity.setOccurredAt(change.occurredAt());
        return entity;
    }

    static DisputeStatusChange toDomain(DisputeStatusAuditEntity entity) { // reconstruye la fila del historial
        return new DisputeStatusChange(
                entity.getId(),
                entity.getDisputeId(),
                entity.getFromStatus() == null ? null : DisputeStatus.valueOf(entity.getFromStatus()),
                DisputeStatus.valueOf(entity.getToStatus()),
                DisputeActorType.valueOf(entity.getActorType()),
                entity.getActorId(),
                entity.getReason(),
                entity.getOccurredAt());
    }

    static AdminReviewEntity toEntity(AdminReview review) { // mapea la revision administrativa
        AdminReviewEntity entity = new AdminReviewEntity();
        entity.setId(review.id());
        entity.setDisputeId(review.disputeId());
        entity.setAdminId(review.adminId());
        entity.setDecision(review.decision().name());
        entity.setReason(review.reason());
        entity.setInternalNotes(review.internalNotes());
        entity.setReviewedAt(review.reviewedAt());
        return entity;
    }

    static AdminReview toDomain(AdminReviewEntity entity) { // reconstruye la revision administrativa
        return new AdminReview(
                entity.getId(),
                entity.getDisputeId(),
                entity.getAdminId(),
                DisputeStatus.valueOf(entity.getDecision()),
                entity.getReason(),
                entity.getInternalNotes(),
                entity.getReviewedAt());
    }

    private static String toJson(List<String> urls) { // guarda la lista como arreglo json en TEXT
        try {
            return JSON.writeValueAsString(urls == null ? List.of() : urls);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to serialize evidence_urls", e);
        }
    }

    private static List<String> fromJson(String json) { // lee el arreglo json persistido
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            String[] values = JSON.readValue(json, String[].class);
            return List.of(values);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to read evidence_urls", e);
        }
    }
}
