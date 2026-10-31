package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.port.out.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 1 (parte B).
@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeFleetInfoAdapter implements FleetInfoPort { // adaptador simulado para consulta de flota en desarrollo

    @Override
    public FleetInfo getVesselInfo(UUID vesselId) { // retorna informacion fija simulada para pruebas locales
        UUID ownerId = UUID.nameUUIDFromBytes(vesselId.toString().getBytes(StandardCharsets.UTF_8)); // genera owner id deterministico
        return new FleetInfo(ownerId, 12, "America/Bogota"); // retorna capacidad doce y zona bogota
    }
}
