package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.application.dto.FleetMaintenanceWindow;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

public interface FleetOperationalStatusPort { // puerto de salida para verificar estado operativo en modulo 1

    /**
     * Indica si la embarcacion se encuentra disponible para el rango de la reserva.
     * El adaptador interpreta el mantenimiento por fechas en la zona del puerto (contrato C3).
     */
    boolean isAvailable(UUID vesselId, LocalDate start, LocalDate end, ZoneId harborZone);

    /**
     * Ventana de mantenimiento vigente reportada por C3, si la hay.
     * La usa el job de reconciliacion para construir un aviso sintetico sin inventar fechas.
     */
    Optional<FleetMaintenanceWindow> maintenanceWindow(UUID vesselId, LocalDate start, LocalDate end, ZoneId harborZone);
}
