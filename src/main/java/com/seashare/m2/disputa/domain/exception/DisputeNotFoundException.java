package com.seashare.m2.disputa.domain.exception;

import com.seashare.m2.shared.domain.DomainException;
import com.seashare.m2.shared.domain.ErrorCode;

import java.util.UUID;

public class DisputeNotFoundException extends DomainException { // la disputa solicitada no existe

    public DisputeNotFoundException(UUID disputeId) { // construye el error 404 del contrato
        super(ErrorCode.DISPUTE_NOT_FOUND.getCode(), "Dispute not found: " + disputeId);
    }
}
