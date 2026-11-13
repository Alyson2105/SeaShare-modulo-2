package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.checkin;

import com.seashare.m2.reservas.application.port.out.CheckinEventPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CheckinEventAdapter implements CheckinEventPort { // adaptador de persistencia del evento de check-in

    private final CheckinEventJpaRepository repository; // repositorio jpa del evento

    public CheckinEventAdapter(CheckinEventJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void save(UUID reservationId, UUID ownerId, Instant actualTime, String notes) { // inserta el evento inmutable
        CheckinEventEntity entity = new CheckinEventEntity(); // crea la entidad
        entity.setReservationId(reservationId); // asigna la reserva
        entity.setOwnerId(ownerId); // asigna el propietario
        entity.setActualTime(actualTime); // asigna la hora real
        entity.setNotes(notes); // asigna las notas
        repository.saveAndFlush(entity); // inserta y fuerza el envio a la base
    }
}