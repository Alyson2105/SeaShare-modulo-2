package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationView(
        UUID reservationId, // identificador unico de la reserva
        String reservationCode, // codigo visible persistido
        ReservationStatus status, // estado actual de la reserva
        CancellationSubtype cancellationSubtype, // subestado de cancelacion
        UUID renterId, // identificador del arrendatario
        UUID ownerId, // identificador del propietario
        UUID vesselId, // identificador de la embarcacion
        LocalDate startDate, // fecha inicial de la reserva
        LocalDate endDate, // fecha final de la reserva
        int passengers, // cantidad de pasajeros
        String holderName, // nombre completo del titular
        String holderPhone, // telefono de contacto del titular
        String holderEmail, // correo electronico del titular
        BigDecimal estimatedTotal, // monto estimado persistido sin operaciones
        BigDecimal rentalAmount, // alquiler liquidado por M3, si existe
        BigDecimal insuranceAmount, // seguro liquidado por M3, si existe
        BigDecimal guaranteeDepositAmount, // deposito liquidado por M3, si existe
        BigDecimal totalAmount, // total liquidado por M3, si existe
        String currency, // codigo de moneda
        LocalTime departureTime, // hora programada de zarpe
        String harborTimezone, // zona horaria del puerto
        Instant ttlExpiresAt, // marca temporal de expiracion de la reserva
        Instant statusChangedAt, // hora del ultimo cambio de estado
        Instant actualDepartureAt, // hora real de zarpe, si existe
        Instant actualArrivalAt, // hora real de desembarque, si existe
        Instant createdAt // marca temporal de creacion del registro
) { // proyeccion inmutable de lectura de la reserva

    public static ReservationView from(Reservation reservation) {
        return new ReservationView(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getStatus(),
                reservation.getCancellationSubtype(),
                reservation.getRenterId(),
                reservation.getOwnerId(),
                reservation.getVesselId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getPassengers(),
                reservation.getHolderName(),
                reservation.getHolderPhone(),
                reservation.getHolderEmail(),
                reservation.getEstimatedTotal(),
                reservation.getRentalAmount(),
                reservation.getInsuranceAmount(),
                reservation.getGuaranteeDepositAmount(),
                reservation.getTotalAmount(),
                reservation.getCurrency(),
                reservation.getDepartureTime(),
                reservation.getHarborTimezone(),
                reservation.getTtlExpiresAt(),
                reservation.getStatusChangedAt(),
                reservation.getActualDepartureAt(),
                reservation.getActualArrivalAt(),
                reservation.getCreatedAt()
        );
    }
}
