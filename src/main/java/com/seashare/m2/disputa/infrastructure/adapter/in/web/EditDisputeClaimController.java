package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.in.EditDisputeClaimUseCase;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class EditDisputeClaimController {
    private final EditDisputeClaimUseCase edit;

    public EditDisputeClaimController(EditDisputeClaimUseCase edit) { this.edit = edit; }

    @PutMapping("/{disputeId}/claims")
    public DisputeClaimResponse edit(@PathVariable String disputeId, @Valid @RequestBody RegisterDisputeClaimRequest request,
                                     DisputeCallerIdentity identity) {
        if (!"OWNER".equalsIgnoreCase(identity.role())) {
            throw new DisputeRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "Only the owner can edit the claim");
        }
        UUID id;
        try { id = UUID.fromString(disputeId.trim()); }
        catch (IllegalArgumentException e) { throw new DisputeRuleException(ErrorCode.INVALID_UUID, "Invalid dispute_id"); }
        return DisputeClaimResponse.edited(edit.edit(new RegisterDisputeClaimCommand(id, identity.userId(),
                request.description(), request.damageCategory(), request.evidenceUrls())));
    }
}
