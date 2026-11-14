package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.port.in.ListDisputesUseCase;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeQueryController {

    private final ListDisputesUseCase listDisputes;

    public DisputeQueryController(ListDisputesUseCase listDisputes) {
        this.listDisputes = listDisputes;
    }

    @GetMapping
    public DisputeListResponse list(
            DisputeCallerIdentity identity,
            @RequestParam(name = "reservation_id", required = false) String reservationId) {
        if (!"ADMIN".equalsIgnoreCase(identity.role()) && !"OWNER".equalsIgnoreCase(identity.role())) {
            throw new DisputeRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED,
                    "Only ADMIN or OWNER roles are authorized to list disputes");
        }

        UUID id = reservationId == null ? null : parseReservationId(reservationId);
        return DisputeListResponse.from(listDisputes.list(identity.userId(), identity.role(), id));
    }

    private UUID parseReservationId(String value) {
        try {
            UUID id = UUID.fromString(value.trim());
            if (!id.toString().equalsIgnoreCase(value.trim())) {
                throw new IllegalArgumentException();
            }
            return id;
        } catch (IllegalArgumentException e) {
            throw new DisputeRuleException(ErrorCode.INVALID_UUID, "Invalid UUID format in reservation_id");
        }
    }
}
