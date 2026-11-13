package com.seashare.m2.disputa.application.port.out;

import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeRepositoryPort { // persistencia del contexto disputa

    Optional<GuaranteeDispute> findById(UUID disputeId); // disputa por identificador

    Optional<GuaranteeDispute> findByReservationId(UUID reservationId); // idempotencia de la apertura

    Optional<DisputeClaim> findClaimByDisputeId(UUID disputeId); // unico reclamo admitido (D-08)

    List<DisputeStatusChange> findStatusHistory(UUID disputeId); // historial inmutable, ordenado por ocurrencia

    GuaranteeDispute save(GuaranteeDispute dispute); // inserta o actualiza la disputa

    DisputeClaim saveClaim(DisputeClaim claim); // inserta el reclamo

    void recordStatusChange(DisputeStatusChange change); // agrega una fila al historial
}
