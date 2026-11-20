package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportNoShowResult( // resultado de CU-05
        UUID reservationId, // reserva cancelada por inasistencia
        ReservationStatus status, // estado resultante
        CancellationSubtype cancellationSubtype, // subtipo NO_SHOW
        OffsetDateTime cancelledAt, // hora de cancelacion en zona del puerto
        long waitMinutesRecorded, // minutos completos esperados
        UUID vesselId, // embarcacion asociada
        OperationalStatus vesselOperationalStatus, // aviso a M1 o null
        String message // resumen informativo para el usuario
) {
}