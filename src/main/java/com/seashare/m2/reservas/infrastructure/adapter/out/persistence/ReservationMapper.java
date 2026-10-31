package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper { // mapeador entre entidad de persistencia y modelo de dominio

    public Reservation toDomain(ReservationEntity entity) { // convierte entidad jpa a agregado de dominio
        if (entity == null) { // valida si la entidad es nula
            return null; // retorna nulo
        }
        return new Reservation( // instancia agregado con los seis campos
                entity.getId(), // identificador
                entity.getRenterId(), // arrendatario
                entity.getOwnerId(), // propietario
                entity.getStatus(), // estado
                entity.getCancellationSubtype(), // subtipo de cancelacion
                entity.getVersion() // version optimista
        );
    }

    public void applyChanges(Reservation domain, ReservationEntity entity) { // aplica unicamente cambios de estado a la entidad
        entity.setStatus(domain.getStatus()); // actualiza solo el estado
        entity.setCancellationSubtype(domain.getCancellationSubtype()); // actualiza solo el subtipo de cancelacion
    }
}
