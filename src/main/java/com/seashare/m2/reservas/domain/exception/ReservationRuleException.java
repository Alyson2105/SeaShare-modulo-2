package com.seashare.m2.reservas.domain.exception;

import com.seashare.m2.shared.domain.DomainException;
import com.seashare.m2.shared.domain.ErrorCode;

public class ReservationRuleException extends DomainException { // excepcion de reglas de negocio para reservas

    public ReservationRuleException(ErrorCode errorCode, String message) { // constructor con codigo y mensaje de regla
        super(errorCode.getCode(), message); // propaga codigo y mensaje a la excepcion de dominio base
    }
}
