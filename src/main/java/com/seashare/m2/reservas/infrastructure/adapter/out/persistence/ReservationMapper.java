package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper { // mapeador entre entidad de persistencia y modelo de dominio

    public Reservation toDomain(ReservationEntity entity) { // convierte entidad jpa a agregado de dominio
        if (entity == null) { // valida si la entidad es nula
            return null; // retorna nulo
        }
        return Reservation.builder()
                .id(entity.getId())
                .renterId(entity.getRenterId())
                .ownerId(entity.getOwnerId())
                .vesselId(entity.getVesselId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .passengers(entity.getPassengers())
                .holderName(entity.getHolderName())
                .holderPhone(entity.getHolderPhone())
                .holderEmail(entity.getHolderEmail())
                .estimatedTotal(entity.getEstimatedTotal())
                .currency(entity.getCurrency())
                .departureTime(entity.getDepartureTime())
                .harborTimezone(entity.getHarborTimezone())
                .ttlExpiresAt(entity.getTtlExpiresAt())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus())
                .cancellationSubtype(entity.getCancellationSubtype())
                .version(entity.getVersion())
                .build();
    }

    public ReservationEntity toNewEntity(Reservation reservation, String reservationCode) { // crea nueva entidad para persistencia
        ReservationEntity entity = new ReservationEntity(); // crea instancia de entidad
        entity.setReservationCode(reservationCode); // genera codigo unico
        entity.setRenterId(reservation.getRenterId()); // asigna arrendatario
        entity.setVesselId(reservation.getVesselId()); // asigna embarcacion
        entity.setOwnerId(reservation.getOwnerId()); // asigna propietario
        entity.setStatus(reservation.getStatus() == null ? ReservationStatus.INITIATED : reservation.getStatus()); // fija estado inicial
        entity.setStartDate(reservation.getStartDate()); // asigna fecha de inicio
        entity.setEndDate(reservation.getEndDate()); // asigna fecha de fin
        entity.setDepartureTime(reservation.getDepartureTime()); // asigna hora de salida
        entity.setPassengers(reservation.getPassengers()); // asigna cantidad de pasajeros
        entity.setHolderName(reservation.getHolderName()); // asigna nombre del titular
        entity.setHolderPhone(reservation.getHolderPhone()); // asigna telefono de contacto
        entity.setHolderEmail(reservation.getHolderEmail()); // asigna correo electronico
        entity.setEstimatedTotal(reservation.getEstimatedTotal()); // almacena monto estimado literal
        entity.setCurrency(reservation.getCurrency() == null ? "COP" : reservation.getCurrency()); // fija moneda en COP
        entity.setTtlExpiresAt(reservation.getTtlExpiresAt()); // asigna expiracion de ttl
        entity.setM1Blocked(false); // indica que no bloquea en modulo 1
        entity.setCreatedAt(reservation.getCreatedAt()); // asigna fecha de creacion provista por reloj
        entity.setHarborTimezone(reservation.getHarborTimezone()); // asigna zona horaria del puerto
        return entity;
    }

    public void applyChanges(Reservation domain, ReservationEntity entity) { // aplica unicamente cambios de estado a la entidad
        entity.setStatus(domain.getStatus()); // actualiza solo el estado
        entity.setCancellationSubtype(domain.getCancellationSubtype()); // actualiza solo el subtipo de cancelacion
    }
}
