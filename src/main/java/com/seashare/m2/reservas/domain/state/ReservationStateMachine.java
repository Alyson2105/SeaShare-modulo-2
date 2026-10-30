package com.seashare.m2.reservas.domain.state;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public class ReservationStateMachine { // maquina de estados de reservas

    private final EnumMap<ReservationStatus, Set<ReservationStatus>> transitions = new EnumMap<>(ReservationStatus.class); // matriz de transiciones

    public ReservationStateMachine() { // inicializa la maquina de estados
        transitions.put(ReservationStatus.INITIATED, EnumSet.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.EXPIRED)); // salidas permitidas de INITIATED
        transitions.put(ReservationStatus.PENDING_PAYMENT, EnumSet.of(ReservationStatus.RESERVED, ReservationStatus.EXPIRED, ReservationStatus.PAYMENT_FAILED)); // salidas permitidas de PENDING_PAYMENT
        transitions.put(ReservationStatus.RESERVED, EnumSet.of(ReservationStatus.IN_NAVIGATION, ReservationStatus.CANCELLED)); // salidas permitidas de RESERVED
        transitions.put(ReservationStatus.IN_NAVIGATION, EnumSet.of(ReservationStatus.COMPLETED)); // salidas permitidas de IN_NAVIGATION
        transitions.put(ReservationStatus.COMPLETED, EnumSet.noneOf(ReservationStatus.class)); // sin salidas para COMPLETED
        transitions.put(ReservationStatus.CANCELLED, EnumSet.noneOf(ReservationStatus.class)); // sin salidas para CANCELLED
        transitions.put(ReservationStatus.EXPIRED, EnumSet.noneOf(ReservationStatus.class)); // sin salidas para EXPIRED
        transitions.put(ReservationStatus.PAYMENT_FAILED, EnumSet.noneOf(ReservationStatus.class)); // sin salidas para PAYMENT_FAILED
    }

    public boolean canTransition(ReservationStatus from, ReservationStatus to) { // evalua si la transicion esta permitida
        if (from == null || to == null) { // valida parametros no nulos
            return false; // retorna falso si alguno es nulo
        }
        Set<ReservationStatus> allowed = transitions.get(from); // obtiene destinos validos
        return allowed != null && allowed.contains(to); // comprueba si el destino esta permitido
    }

    public void validate(ReservationStatus from, ReservationStatus to, CancellationSubtype subtype) { // valida transicion y reglas de negocio
        if (!canTransition(from, to)) { // si la transicion no esta permitida
            throw new InvalidTransitionException(from, to, "Cannot transition from " + from + " to " + to); // lanza excepcion por estado invalido
        }
        if (to == ReservationStatus.CANCELLED && subtype == null) { // cancelacion sin subtipo
            throw new InvalidTransitionException(from, to, "Cancellation subtype is required when transitioning to CANCELLED"); // lanza excepcion por falta de subtipo
        }
    }
}
