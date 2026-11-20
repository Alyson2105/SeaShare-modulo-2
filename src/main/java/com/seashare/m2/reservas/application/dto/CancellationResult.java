package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CancellationResult( // resultado completo de la cancelacion
        UUID reservationId, // reserva cancelada
        ReservationStatus status, // estado resultante
        CancellationSubtype cancellationSubtype, // clasificacion aplicada
        ActorType requestingActor, // actor que solicito la cancelacion
        Double anticipationHours, // anticipacion en horas; null para OWNER
        String portTimezone, // zona horaria IANA del puerto
        OffsetDateTime scheduledDeparture, // zarpe programado con offset
        OffsetDateTime cancelledAt, // instante de la solicitud con offset
        String reason, // motivo del propietario; null para RENTER
        String justification, // justificacion normalizada
        UUID vesselId, // embarcacion asociada
        OperationalStatus vesselNewOperationalStatus, // estado operativo a notificar a M1; puede ser null
        String m1SyncStatus, // ENQUEUED si se encolo el aviso a M1; null si no
        String m3NotificationStatus // PUBLISHED si se encolo el evento a M3; null si no
) {
}