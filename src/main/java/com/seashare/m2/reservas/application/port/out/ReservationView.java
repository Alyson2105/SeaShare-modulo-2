package com.seashare.m2.reservas.application.port.out;

import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationView(
        UUID reservationId, // identificador unico de la reserva
        ReservationStatus status, // estado actual de la reserva
        UUID vesselId, // identificador de la embarcacion
        LocalDate startDate, // fecha inicial de la reserva
        LocalDate endDate, // fecha final de la reserva
        int passengers, // cantidad de pasajeros
        String holderName, // nombre completo del titular
        String holderPhone, // telefono de contacto del titular
        String holderEmail, // correo electronico del titular
        BigDecimal estimatedTotal, // monto estimado persistido sin operaciones
        String currency, // codigo de moneda
        LocalTime departureTime, // hora programada de zarpe
        String harborTimezone, // zona horaria del puerto
        Instant ttlExpiresAt, // marca temporal de expiracion de la reserva
        Instant createdAt // marca temporal de creacion del registro
) { // proyeccion inmutable de lectura de la reserva
}
