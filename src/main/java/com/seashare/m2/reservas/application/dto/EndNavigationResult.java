package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EndNavigationResult( // resultado de CU-07
        UUID reservationId, // reserva completada
        ReservationStatus status, // estado resultante
        OffsetDateTime actualArrivalAt, // llegada real en zona del puerto
        UUID vesselId, // embarcacion asociada
        OperationalStatus vesselOperationalStatus, // aviso a M1 o null
        UUID disputeId, // siempre null hasta CU-16
        OffsetDateTime disputeWindowExpiresAt, // siempre null hasta CU-16
        String message // resumen informativo para el usuario
) {
}