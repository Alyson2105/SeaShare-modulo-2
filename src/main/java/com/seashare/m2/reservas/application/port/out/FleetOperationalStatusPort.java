package com.seashare.m2.reservas.application.port.out;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public interface FleetOperationalStatusPort { // puerto de salida para verificar estado operativo en modulo 1

    /**
     * Indica si la embarcacion se encuentra disponible para el rango de la reserva.
     * El adaptador interpreta el mantenimiento por fechas en la zona del puerto (contrato C3).
     */
    boolean isAvailable(UUID vesselId, LocalDate start, LocalDate end, ZoneId harborZone);
}
