package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusResult;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeAdminController { // resolucion administrativa de la disputa (CU-17)

    static final String ADMIN_ROLE = "ADMIN"; // unico perfil autorizado a resolver

    private final UpdateDisputeStatusUseCase updateDisputeStatus; // caso de uso de transicion

    public DisputeAdminController(UpdateDisputeStatusUseCase updateDisputeStatus) {
        this.updateDisputeStatus = updateDisputeStatus;
    }

    @PutMapping("/{disputeId}/status")
    public UpdateDisputeStatusResponse update(@Valid @RequestBody UpdateDisputeStatusRequest request,
                                              DisputeCallerIdentity identity,
                                              @PathVariable("disputeId") String disputeId) {
        if (!ADMIN_ROLE.equalsIgnoreCase(identity.role())) { // solo el perfil Administrador
            throw new DisputeRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only the ADMIN role is authorized to resolve disputes");
        }

        UUID id;
        try {
            id = UUID.fromString(disputeId.trim()); // valida el uuid de la ruta
        } catch (IllegalArgumentException e) {
            throw new DisputeRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in dispute_id");
        }

        DisputeStatus target;
        try {
            target = DisputeStatus.valueOf(request.newStatus().trim().toUpperCase(Locale.ROOT)); // destino del contrato
        } catch (IllegalArgumentException e) {
            throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS,
                    "new_status must be ACCEPTED, REJECTED or PENDING");
        }

        UpdateDisputeStatusResult result = updateDisputeStatus.update(new UpdateDisputeStatusCommand(
                id, target, identity.userId(), request.reason(), request.internalNotes(), DisputeActorType.ADMIN));
        return UpdateDisputeStatusResponse.from(result);
    }
}
