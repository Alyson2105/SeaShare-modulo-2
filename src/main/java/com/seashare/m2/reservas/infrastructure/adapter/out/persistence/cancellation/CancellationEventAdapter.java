package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.cancellation;

import com.seashare.m2.reservas.application.dto.CancellationEventRecord;
import com.seashare.m2.reservas.application.port.out.CancellationEventPort;
import org.springframework.stereotype.Component;

@Component
public class CancellationEventAdapter implements CancellationEventPort { // adaptador de persistencia del evento de cancelacion

    private final CancellationEventJpaRepository repository; // repositorio jpa del evento

    public CancellationEventAdapter(CancellationEventJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void save(CancellationEventRecord record) { // inserta el evento inmutable
        CancellationEventEntity entity = new CancellationEventEntity(); // crea la entidad
        entity.setReservationId(record.reservationId()); // asigna la reserva
        entity.setActor(record.actor().name()); // guarda el nombre del actor
        entity.setRequestedAt(record.requestedAt()); // asigna el instante de la solicitud
        entity.setAnticipationMinutes(record.anticipationMinutes()); // asigna los minutos
        entity.setCancellationSubtype(record.cancellationSubtype()); // asigna el subtipo
        entity.setReason(record.reason()); // asigna el motivo
        entity.setJustification(record.justification()); // asigna la justificacion
        repository.save(entity); // persiste solo insercion
    }
}