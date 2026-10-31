package com.seashare.m2.reservas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateReservationCommand(
        UUID renterId, // identificador del arrendatario autenticado
        String idempotencyKey, // clave de idempotencia de la solicitud
        UUID vesselId, // identificador de la embarcacion
        LocalDate startDate, // fecha inicial de la reserva
        LocalDate endDate, // fecha final de la reserva
        int passengers, // cantidad de pasajeros
        String holderName, // nombre completo del titular
        String holderPhone, // telefono de contacto del titular
        String holderEmail, // correo electronico del titular
        LocalTime departureTime, // hora programada de zarpe
        BigDecimal estimatedTotal // total estimado sin operaciones aritmeticas
) { // comando inmutable para iniciar la creacion de una reserva
}
