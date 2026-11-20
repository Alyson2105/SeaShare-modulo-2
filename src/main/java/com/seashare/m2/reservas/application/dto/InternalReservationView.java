package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record InternalReservationView( // datos crudos de la reserva leidos para Modulo 3 (sin dinero)
        UUID reservationId, // identificador de la reserva
        String reservationCode, // codigo de negocio de la reserva
        UUID renterId, // identificador del arrendatario
        UUID vesselId, // identificador de la embarcacion
        UUID ownerId, // identificador del propietario
        LocalDate startDate, // fecha de inicio pactada
        LocalDate endDate, // fecha de fin pactada
        LocalTime departureTime, // hora de zarpe pactada
        String harborTimezone, // zona horaria del puerto
        int passengers, // cantidad de pasajeros
        ReservationStatus status, // estado interno de la reserva
        CancellationSubtype cancellationSubtype, // subtipo de cancelacion si aplica
        Instant ttlExpiresAt, // vencimiento del temporizador ttl
        Instant actualDepartureAt, // check-in real en muelle
        Instant actualArrivalAt, // check-out real en muelle
        String completionNotes, // novedades reportadas al cierre
        Instant createdAt, // creacion del registro
        Instant updatedAt, // ultima actualizacion del registro
        String latestCancellationActor, // actor crudo de la auditoria mas reciente CANCELLED
        int durationDays, // dias completos del alquiler (calculado en el servicio)
        int durationNights, // noches del alquiler (calculado en el servicio)
        String cancellationActor, // actor efectivo expuesto a Modulo 3
        Long cancellationAnticipationMinutes, // anticipacion registrada en cancellation_event; nula sin evento
        boolean ttlApplies, // indica si el temporizador ttl esta vigente
        Long ttlRemainingSeconds // segundos restantes del ttl; nulo si no aplica
) { // proyeccion inmutable de solo lectura para el endpoint interno

    public InternalReservationView withDerived(int durationDays, int durationNights, String cancellationActor,
                                               boolean ttlApplies, Long ttlRemainingSeconds) { // agrega valores calculados
        return new InternalReservationView(reservationId, reservationCode, renterId, vesselId, ownerId,
                startDate, endDate, departureTime, harborTimezone, passengers, status, cancellationSubtype,
                ttlExpiresAt, actualDepartureAt, actualArrivalAt, completionNotes, createdAt, updatedAt,
                latestCancellationActor, durationDays, durationNights, cancellationActor,
                cancellationAnticipationMinutes, ttlApplies, ttlRemainingSeconds); // copia con los campos derivados
    }
}
