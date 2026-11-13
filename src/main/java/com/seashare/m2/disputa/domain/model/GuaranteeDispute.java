package com.seashare.m2.disputa.domain.model;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.valueobject.ClaimWindow;
import com.seashare.m2.shared.domain.ErrorCode;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado raiz del contexto disputa (CU-16, CU-17).
 *
 * <p>Objeto de dominio puro: sin Spring, JPA ni AMQP. Su estado
 * (PENDING/REJECTED/ACCEPTED) es independiente del estado de la reserva
 * (nota de dominio de CU-16 y CU-17).</p>
 */
public final class GuaranteeDispute { // disputa de garantia de una reserva completada

    private final UUID id; // identificador de la disputa

    private final UUID reservationId; // reserva que origino la disputa

    private final UUID ownerId; // propietario registrado; autoriza el reclamo sin leer reservas

    private final ClaimWindow window; // ventana fija de 24 horas

    private DisputeStatus status; // estado actual de la disputa

    private boolean hasClaim; // indica si ya se registro el unico reclamo admitido

    private String reason; // motivo informativo local; nunca se publica a M3

    private UUID resolvedBy; // admin que resolvio; nulo en el cierre de sistema

    private Instant resolvedAt; // instante de la resolucion; nulo mientras PENDING

    private GuaranteeDispute(UUID id,
                             UUID reservationId,
                             UUID ownerId,
                             ClaimWindow window,
                             DisputeStatus status,
                             boolean hasClaim,
                             String reason,
                             UUID resolvedBy,
                             Instant resolvedAt) { // constructor privado: se crea con open o restore
        this.id = id;
        this.reservationId = reservationId;
        this.ownerId = ownerId;
        this.window = window;
        this.status = status;
        this.hasClaim = hasClaim;
        this.reason = reason;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public static GuaranteeDispute open(UUID reservationId, UUID ownerId, Instant completedAt) { // apertura automatica (CU-16 FR-001)
        if (reservationId == null || ownerId == null || completedAt == null) { // datos minimos del contrato
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS,
                    "reservationId, ownerId and completedAt are required to open a dispute");
        }
        return new GuaranteeDispute(UUID.randomUUID(), reservationId, ownerId,
                ClaimWindow.startingAt(completedAt), DisputeStatus.PENDING, false, null, null, null);
    }

    public static GuaranteeDispute restore(UUID id,
                                           UUID reservationId,
                                           UUID ownerId,
                                           Instant windowStart,
                                           Instant windowEnd,
                                           DisputeStatus status,
                                           boolean hasClaim,
                                           String reason,
                                           UUID resolvedBy,
                                           Instant resolvedAt) { // reconstruye el agregado desde persistencia
        return new GuaranteeDispute(id, reservationId, ownerId, new ClaimWindow(windowStart, windowEnd),
                status, hasClaim, reason, resolvedBy, resolvedAt);
    }

    public boolean canRegisterClaim(Instant now) { // solo PENDING, sin reclamo previo y con ventana abierta
        return status == DisputeStatus.PENDING && !hasClaim && window.isOpenAt(now);
    }

    public boolean isFinal() { // REJECTED y ACCEPTED son terminales sin reapertura
        return status.isFinal();
    }

    public boolean isExpired(Instant now) { // la ventana ya cerro en el instante exacto
        return window.isClosedAt(now);
    }

    public void registerClaim() { // el reclamo no cambia el estado (CU-16 FR-006)
        this.hasClaim = true;
    }

    public UUID id() {
        return id;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public ClaimWindow window() {
        return window;
    }

    public DisputeStatus status() {
        return status;
    }

    public boolean hasClaim() {
        return hasClaim;
    }

    public String reason() {
        return reason;
    }

    public UUID resolvedBy() {
        return resolvedBy;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }
}
