package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.reservation;

import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
class ReservationMapper { // mapeador entre entidad de persistencia y modelo de dominio

    public Reservation toDomain(ReservationEntity entity) { // convierte entidad jpa a agregado de dominio
        if (entity == null) { // valida si la entidad es nula
            return null; // retorna nulo
        }
        Reservation domain = Reservation.builder()
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
        domain.setCompletionNotes(entity.getCompletionNotes()); // mapea notas de cierre
        domain.setPaymentReference(entity.getPaymentReference()); // mapea referencia de pago
        domain.setPaymentConfirmedAt(entity.getPaymentConfirmedAt()); // mapea confirmacion de pago
        domain.setStatusChangedAt(entity.getStatusChangedAt()); // mapea hora del cambio de estado
        domain.setActualDepartureAt(entity.getActualDepartureAt()); // mapea hora real de zarpe (solo persistencia)
        domain.setActualArrivalAt(entity.getActualArrivalAt()); // mapea hora real de desembarque (solo persistencia)
        domain.setM1Blocked(entity.isM1Blocked()); // mapea la guarda de bloqueo en M1
        domain.setReservationCode(entity.getReservationCode()); // mapea el codigo visible persistido
        domain.setRentalAmount(entity.getRentalAmount()); // mapea alquiler liquidado (solo lectura)
        domain.setInsuranceAmount(entity.getInsuranceAmount()); // mapea seguro liquidado (solo lectura)
        domain.setGuaranteeDepositAmount(entity.getGuaranteeDepositAmount()); // mapea deposito liquidado (solo lectura)
        domain.setTotalAmount(entity.getTotalAmount()); // mapea total liquidado (solo lectura)
        return domain; // retorna agregado completo
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
        entity.setStatusChangedAt(reservation.getCreatedAt()); // la creacion es el primer cambio de estado (V4 NOT NULL)
        entity.setHarborTimezone(reservation.getHarborTimezone()); // asigna zona horaria del puerto
        return entity;
    }

    public void applyChanges(Reservation domain, ReservationEntity entity) { // aplica cambios de estado y campos de CU-08 a la entidad
        entity.setStatus(domain.getStatus()); // actualiza el estado
        entity.setCancellationSubtype(domain.getCancellationSubtype()); // actualiza el subtipo de cancelacion
        if (domain.getStatusChangedAt() != null) { // hora del cambio siempre viene del servicio
            entity.setStatusChangedAt(domain.getStatusChangedAt()); // hora exacta del cambio (V4)
        }
        if (domain.getCompletionNotes() != null) { // solo si el dominio aporto novedades
            entity.setCompletionNotes(domain.getCompletionNotes()); // notas de cierre en COMPLETED
        }
        if (domain.getPaymentReference() != null) { // solo si el dominio aporto referencia
            entity.setPaymentReference(domain.getPaymentReference()); // referencia del cobro en RESERVED
        }
        if (domain.getPaymentConfirmedAt() != null) { // solo si el dominio aporto confirmacion
            entity.setPaymentConfirmedAt(domain.getPaymentConfirmedAt()); // hora de confirmacion del pago
        }
        if (domain.getActualDepartureAt() != null) { // round-trip de hora real de zarpe (la fija CU-06)
            entity.setActualDepartureAt(domain.getActualDepartureAt()); // persiste salida real
        }
        if (domain.getActualArrivalAt() != null) { // round-trip de hora real de desembarque (la fija CU-07)
            entity.setActualArrivalAt(domain.getActualArrivalAt()); // persiste llegada real
        }
        if (domain.getRentalAmount() != null) { // CU-03: desglose literal de M3
            entity.setRentalAmount(domain.getRentalAmount()); // alquiler sin recalcular
        }
        if (domain.getInsuranceAmount() != null) { // CU-03: seguro literal de M3
            entity.setInsuranceAmount(domain.getInsuranceAmount()); // seguro sin recalcular
        }
        if (domain.getGuaranteeDepositAmount() != null) { // CU-03: deposito literal de M3
            entity.setGuaranteeDepositAmount(domain.getGuaranteeDepositAmount()); // deposito sin recalcular
        }
        if (domain.getTotalAmount() != null) { // CU-03: total liquidado literal de M3
            entity.setTotalAmount(domain.getTotalAmount()); // total sin recalcular
        }
        entity.setM1Blocked(domain.isM1Blocked()); // persiste la guarda de notificacion a M1 (T-08)
    }
}
