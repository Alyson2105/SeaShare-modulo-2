package com.seashare.m2.reservas.domain.state;

import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.DomainException;

public class InvalidTransitionException extends DomainException { // transicion no permitida por la matriz

    public static final String CODE = "INVALID_STATE_TRANSITION"; // codigo interno del catalogo

    private final ReservationStatus from; // estado de origen
    private final ReservationStatus to; // estado de destino pedido

    public InvalidTransitionException(ReservationStatus from, ReservationStatus to, String detail) {
        super(CODE, detail); // detalle legible del motivo
        this.from = from; // guarda origen
        this.to = to; // guarda destino
    }

    public ReservationStatus getFrom() {
        return from; // estado de origen
    }

    public ReservationStatus getTo() {
        return to; // estado de destino
    }
}
