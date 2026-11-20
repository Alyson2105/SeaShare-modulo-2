package com.seashare.m2.reservas.application.dto;

import java.time.Instant;

/** Ventana de mantenimiento devuelta por C3, usada por el job de reconciliacion. */
public record FleetMaintenanceWindow(Instant start, Instant until) {
}
