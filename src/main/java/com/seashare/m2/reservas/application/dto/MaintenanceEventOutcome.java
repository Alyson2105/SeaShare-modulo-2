package com.seashare.m2.reservas.application.dto;

/** Resultado funcional de un aviso de mantenimiento C5 (contrato M1-M2 §6). */
public enum MaintenanceEventOutcome {
    APPLIED, // se cancelo al menos una reserva
    DUPLICATE, // el event_id ya se proceso
    NO_AFFECTED_RESERVATIONS, // no hay reservas RESERVED que crucen la ventana
    UNKNOWN_VESSEL, // M2 no tiene historial de la embarcacion
    INVALID_RANGE, // rango bien formado pero integramente en el pasado
    AUTO_CANCEL_DISABLED // la cancelacion automatica esta desactivada
}
