package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.noshow;

import com.seashare.m2.reservas.application.port.out.NoShowEventPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NoShowEventAdapter implements NoShowEventPort { // adaptador de persistencia del evento de inasistencia

    private final NoShowEventJpaRepository repository; // repositorio jpa del evento

    public NoShowEventAdapter(NoShowEventJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void save(UUID reservationId, UUID ownerId, Instant reportedAt, int waitingMinutes, String observations) { // inserta el evento inmutable
        NoShowEventEntity entity = new NoShowEventEntity(); // crea la entidad
        entity.setReservationId(reservationId); // asigna la reserva
        entity.setOwnerId(ownerId); // asigna el propietario
        entity.setReportedAt(reportedAt); // asigna la hora del reporte
        entity.setWaitingMinutes(waitingMinutes); // asigna los minutos esperados
        entity.setObservations(observations); // asigna las observaciones
        repository.saveAndFlush(entity); // inserta y fuerza el envio a la base
    }
}