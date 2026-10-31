package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 1 (parte B).
@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeFleetOperationalStatusAdapter implements FleetOperationalStatusPort { // adaptador simulado para estado operativo en desarrollo

    @Override
    public boolean isAvailable(UUID vesselId) { // evalua disponibilidad operativa simulada
        return true; // responde siempre disponible en modo desarrollo
    }
}
