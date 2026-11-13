package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.in.RegisterDisputeClaimUseCase;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeClaimController { // registro del reclamo del propietario (CU-16)

    static final String OWNER_ROLE = "OWNER"; // unico perfil autorizado a reclamar

    private final RegisterDisputeClaimUseCase registerClaim; // caso de uso del reclamo

    public DisputeClaimController(RegisterDisputeClaimUseCase registerClaim) {
        this.registerClaim = registerClaim;
    }

    @PostMapping("/{disputeId}/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeClaimResponse register(@Valid @RequestBody RegisterDisputeClaimRequest request,
                                         DisputeCallerIdentity identity,
                                         @PathVariable("disputeId") String disputeId) {
        if (!OWNER_ROLE.equalsIgnoreCase(identity.role())) { // solo el propietario registrado puede reclamar
            throw new DisputeRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only the OWNER role is authorized to file a claim");
        }

        UUID id;
        try {
            id = UUID.fromString(disputeId.trim()); // valida el formato uuid de la ruta
        } catch (IllegalArgumentException e) {
            throw new DisputeRuleException(ErrorCode.INVALID_UUID,
                    "Invalid UUID format in dispute_id");
        }

        DisputeClaimView view = registerClaim.register(new RegisterDisputeClaimCommand(
                id, identity.userId(), request.description(), request.damageCategory(), request.evidenceUrls()));
        return DisputeClaimResponse.from(view);
    }
}
