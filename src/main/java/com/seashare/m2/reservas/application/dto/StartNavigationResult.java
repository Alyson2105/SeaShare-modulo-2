package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StartNavigationResult( // resultado de CU-06
        UUID reservationId, // reserva en navegacion
        ReservationStatus status, // estado resultante
        OffsetDateTime actualDepartureAt, // salida real en zona del puerto
        UUID vesselId, // embarcacion asociada
        OperationalStatus vesselOperationalStatus, // aviso a M1 o null
        boolean insuranceActivated, // true si se encolo el evento a M3
        String message // resumen informativo para el usuario
) {
}