package com.seashare.m2.disputa.domain.exception;

import com.seashare.m2.shared.domain.DomainException;
import com.seashare.m2.shared.domain.ErrorCode;

public class DisputeRuleException extends DomainException { // regla de negocio incumplida en el contexto disputa

    public DisputeRuleException(ErrorCode errorCode, String message) { // codigo del catalogo comun + mensaje
        super(errorCode.getCode(), message);
    }
}
