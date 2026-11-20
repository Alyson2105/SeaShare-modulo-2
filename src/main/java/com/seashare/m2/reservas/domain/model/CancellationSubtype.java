package com.seashare.m2.reservas.domain.model;

public enum CancellationSubtype { // motivo de una cancelacion
    FLEXIBLE, // cancelacion con politica flexible
    MODERATE, // cancelacion con politica moderada
    LATE, // cancelacion tardia
    BY_OWNER, // cancelada por el propietario
    NO_SHOW // inasistencia del arrendatario
}