package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.OperationalStatus;

import java.time.Instant;
import java.util.UUID;

public record OperationalStatusEvent( // notificacion de estado operativo a Modulo 1 (CU-22 Key Entities L143)
        UUID eventId, // identificador unico por transicion (CU-22 FR-004)
        UUID vesselId, // embarcacion en Modulo 1
        OperationalStatus operationalStatus, // estado operativo ya mapeado (CU-22 FR-002)
        Instant occurredAt // hora exacta de la transicion (ISO 8601)
) {
}
