package com.seashare.m2.disputa.domain.model;

public enum DisputeStatus { // estados del objeto disputa de garantia (distintos del estado de la reserva)

    PENDING, // ventana abierta o reclamo en revision

    REJECTED, // reclamo improcedente o ausencia de reclamo al vencer la ventana

    ACCEPTED; // reclamo procedente; M3 liquida el deposito al propietario

    public boolean isFinal() { // REJECTED y ACCEPTED no admiten reapertura (CU-17 FR-003)
        return this == REJECTED || this == ACCEPTED;
    }
}
