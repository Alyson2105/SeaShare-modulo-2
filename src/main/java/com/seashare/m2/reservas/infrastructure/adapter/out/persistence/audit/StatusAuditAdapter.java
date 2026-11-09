package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.audit;

import com.seashare.m2.reservas.application.dto.StatusAuditEntry;
import com.seashare.m2.reservas.application.port.out.StatusAuditPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StatusAuditAdapter implements StatusAuditPort { // adaptador de persistencia para auditoria de estado

    private final ReservationStatusAuditJpaRepository repository; // repositorio jpa de auditoria

    public StatusAuditAdapter(ReservationStatusAuditJpaRepository repository) { // constructor con dependencias
        this.repository = repository; // asigna repositorio
    }

    @Override
    public void record(StatusAuditEntry entry) { // registra la transicion en el historial inmutable
        ReservationStatusAuditEntity entity = new ReservationStatusAuditEntity(); // crea nueva entidad
        entity.setReservationId(entry.reservationId()); // asigna id de reserva
        entity.setFromStatus(entry.fromStatus()); // asigna estado de origen
        entity.setToStatus(entry.toStatus()); // asigna estado de destino
        entity.setCancellationSubtype(entry.subtype()); // asigna subtipo de cancelacion
        entity.setActor(entry.actor().name()); // asigna nombre del actor
        entity.setReason(entry.reason()); // asigna motivo
        entity.setOccurredAt(entry.occurredAt()); // asigna marca temporal
        entity.setActorId(entry.actorId()); // asigna id del usuario (V4)
        entity.setEventIds(serializeEventIds(entry.eventIds())); // asigna JSON array de eventos (V4)
        repository.save(entity); // persiste unicamente mediante insert
    }

    private String serializeEventIds(List<UUID> eventIds) { // serializa la lista de eventos a JSON array
        if (eventIds == null || eventIds.isEmpty()) { // sin eventos
            return "[]"; // array vacio
        }
        return eventIds.stream() // recorre los ids
                .map(UUID::toString) // convierte a texto
                .map(id -> "\"" + id + "\"") // entrecomilla para JSON
                .collect(Collectors.joining(",", "[", "]")); // une como array json
    }
}
