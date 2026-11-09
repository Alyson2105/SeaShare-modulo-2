package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChangeReservationStatusResult( // resultado tipado de la transicion (contrato de respuesta de CU-08)
        UUID reservationId, // identificador de la reserva
        ReservationStatus status, // estado actual tras el cambio
        CancellationSubtype cancellationSubtype, // subtipo de cancelacion o null
        Instant statusChangedAt, // hora exacta del cambio
        Instant ttlExpiresAt, // expiracion del ttl; nunca cambia desde la creacion
        Long version, // version incrementada tras el cambio
        List<String> eventsEnqueued // destinos con evento encolado: AMQP, M1_HTTP; puede ser vacio
) {
}
