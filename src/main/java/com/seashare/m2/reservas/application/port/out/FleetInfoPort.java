package com.seashare.m2.reservas.application.port.out;

import java.util.UUID;

public interface FleetInfoPort { // puerto de salida para consultar datos de la embarcacion en modulo 1

    FleetInfo getVesselInfo(UUID vesselId); // obtiene informacion basica y capacidad de la embarcacion
}
