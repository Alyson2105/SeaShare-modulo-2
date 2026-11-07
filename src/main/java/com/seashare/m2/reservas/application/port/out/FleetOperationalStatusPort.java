package com.seashare.m2.reservas.application.port.out;

import java.util.UUID;

public interface FleetOperationalStatusPort { // puerto de salida para verificar estado operativo en modulo 1

    boolean isAvailable(UUID vesselId); // valida si la embarcacion se encuentra disponible operativamente
}
