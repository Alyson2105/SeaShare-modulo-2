package com.seashare.m2.disputa.domain.state;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.shared.domain.ErrorCode;

/**
 * Maquina de estados de la disputa (CU-17).
 *
 * <p>Desde PENDING solo se puede mantener PENDING (revision en curso), rechazar
 * o aceptar. REJECTED y ACCEPTED son finales sin reapertura.</p>
 */
public final class DisputeStateMachine { // reglas de transicion del objeto disputa

    public void validateTransition(DisputeStatus current, DisputeStatus target, boolean hasClaim) { // valida el destino
        if (current.isFinal()) { // FR-003: los estados finales no admiten cambio
            throw new DisputeRuleException(ErrorCode.DISPUTE_ALREADY_RESOLVED,
                    "The dispute has already reached a final state");
        }
        if (target == null) { // destino obligatorio
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS, "Target status is required");
        }
        if (target == DisputeStatus.ACCEPTED && !hasClaim) { // aceptar exige reclamo registrado
            throw new DisputeRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                    "A dispute can only be ACCEPTED when the owner has registered a claim");
        }
    }
}
