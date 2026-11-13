package com.seashare.m2.disputa.application.port.out;

import com.seashare.m2.disputa.domain.model.AdminReview;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeRepositoryPort { // persistencia del contexto disputa

    Optional<GuaranteeDispute> findById(UUID disputeId); // disputa por identificador

    Optional<GuaranteeDispute> findByReservationId(UUID reservationId); // idempotencia de la apertura

    Optional<DisputeClaim> findClaimByDisputeId(UUID disputeId); // unico reclamo admitido (D-08)

    List<DisputeStatusChange> findStatusHistory(UUID disputeId); // historial inmutable, ordenado por ocurrencia

    List<UUID> findPendingDue(Instant now, int limit); // disputas PENDING vencidas y sin reclamo (job de cierre)

    List<AdminReview> findAdminReviews(UUID disputeId); // revisiones administrativas, ordenadas por fecha

    GuaranteeDispute save(GuaranteeDispute dispute); // inserta o actualiza la disputa (flush inmediato)

    DisputeClaim saveClaim(DisputeClaim claim); // inserta el reclamo

    void saveAdminReview(AdminReview review); // agrega la revision administrativa

    void recordStatusChange(DisputeStatusChange change); // agrega una fila al historial
}
