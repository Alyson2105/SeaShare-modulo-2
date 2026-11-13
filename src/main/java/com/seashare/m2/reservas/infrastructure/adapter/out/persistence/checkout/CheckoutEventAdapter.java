package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.checkout;

import com.seashare.m2.reservas.application.port.out.CheckoutEventPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class CheckoutEventAdapter implements CheckoutEventPort { // adaptador de persistencia del evento de checkout

    private final CheckoutEventJpaRepository repository; // repositorio jpa del evento

    public CheckoutEventAdapter(CheckoutEventJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void save(UUID reservationId, UUID ownerId, Instant actualTime, String remarks) { // inserta el evento inmutable
        CheckoutEventEntity entity = new CheckoutEventEntity(); // crea la entidad
        entity.setReservationId(reservationId); // asigna la reserva
        entity.setOwnerId(ownerId); // asigna el propietario
        entity.setActualTime(actualTime); // asigna la hora real
        entity.setRemarks(remarks); // asigna las novedades
        repository.saveAndFlush(entity); // inserta y fuerza el envio a la base
    }
}