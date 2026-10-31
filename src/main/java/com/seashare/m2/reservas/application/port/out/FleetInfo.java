package com.seashare.m2.reservas.application.port.out;

import java.util.UUID;

public record FleetInfo(
        UUID ownerId, // identificador del propietario de la embarcacion
        int maxCapacity, // capacidad maxima de pasajeros
        String harborTimezone // zona horaria del puerto base
) { // datos inmutables de consulta de embarcacion provistos por modulo 1
}
