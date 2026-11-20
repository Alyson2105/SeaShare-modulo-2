package com.seashare.m2.reservas.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationInfoProvidedEvent( // evento de aplicacion para publicar informacion de reserva a modulo 3
        UUID eventId, // identificador deterministico del evento
        UUID reservationId, // identificador unico de la reserva
        UUID vesselId, // identificador interno de la embarcacion
        LocalDate startDate, // fecha de inicio de la reserva
        LocalDate endDate, // fecha de fin de la reserva
        int passengers, // cantidad de pasajeros
        UUID ownerId, // identificador del propietario obtenido de modulo 1
        int maxCapacity, // capacidad maxima informada por modulo 1
        Instant occurredAt // marca temporal del evento obtenida de clockPort
) {
}
