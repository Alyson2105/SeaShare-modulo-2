package com.seashare.m2.reservas.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NewReservation(
        UUID renterId, // identificador del arrendatario
        UUID vesselId, // identificador de la embarcacion
        UUID ownerId, // identificador del propietario obtenido de modulo 1
        LocalDate startDate, // fecha inicial de la reserva
        LocalDate endDate, // fecha final de la reserva
        int passengers, // cantidad de pasajeros
        String holderName, // nombre completo del titular
        String holderPhone, // telefono de contacto del titular
        String holderEmail, // correo electronico del titular
        BigDecimal estimatedTotal, // total estimado persistido literalmente
        LocalTime departureTime, // hora programada de zarpe
        String harborTimezone, // zona horaria del puerto
        Instant createdAt, // instante de creacion fijado por el reloj
        Instant ttlExpiresAt // instante de expiracion calculado con ttl
) { // estructura inmutable con los datos necesarios para persistir una nueva reserva
}
