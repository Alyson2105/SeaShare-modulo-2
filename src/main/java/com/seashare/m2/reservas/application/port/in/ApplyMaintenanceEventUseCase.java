package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.MaintenanceEventCommand;
import com.seashare.m2.reservas.application.dto.MaintenanceEventResult;

public interface ApplyMaintenanceEventUseCase { // caso de uso de C5

    MaintenanceEventResult apply(MaintenanceEventCommand command); // aplica un aviso de mantenimiento
}
