package com.seashare.m2.reservas.domain.exception;

import com.seashare.m2.shared.domain.DomainException;

import java.util.UUID;

public class ReservationNotFoundException extends DomainException { // excepcion cuando la reserva no existe

    public static final String CODE = "RESERVATION_NOT_FOUND"; // codigo interno del catalogo

    public ReservationNotFoundException(UUID id) { // constructor con identificador
        super(CODE, "Reservation not found with id: " + id); // mensaje descriptivo con id
    }

    public ReservationNotFoundException(String message) { // constructor con mensaje directo
        super(CODE, message); // mensaje descriptivo
    }
}
