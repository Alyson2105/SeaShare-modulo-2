package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.seashare.m2.reservas.application.dto.FleetMaintenanceWindow;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

// FALSO SOLO PARA DESARROLLO: lo reemplaza el adaptador HTTP real de Modulo 1 (parte B).
@Component
@ConditionalOnProperty(prefix = "seashare.fleet", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeFleetOperationalStatusAdapter implements FleetOperationalStatusPort { // adaptador simulado para estado operativo en desarrollo

    @Override
    public boolean isAvailable(UUID vesselId, LocalDate start, LocalDate end, ZoneId harborZone) { // evalua disponibilidad operativa simulada
        return true; // responde siempre disponible en modo desarrollo
    }

    @Override
    public Optional<FleetMaintenanceWindow> maintenanceWindow(UUID vesselId, LocalDate start, LocalDate end,
                                                              ZoneId harborZone) { // sin mantenimiento simulado
        return Optional.empty(); // el modo fake no reporta ventanas
    }
}
