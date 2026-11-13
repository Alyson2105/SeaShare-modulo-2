package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.noshow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "noshow_event")
@Immutable
class NoShowEventEntity { // entidad jpa inmutable para el evento de inasistencia

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId; // reserva reportada como inasistencia

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; // propietario que reporto la inasistencia

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt; // instante del reporte

    @Column(name = "waiting_minutes", nullable = false)
    private int waitingMinutes; // minutos completos esperados

    @Column(name = "observations")
    private String observations; // observaciones del propietario

    public NoShowEventEntity() { // constructor sin argumentos requerido por jpa
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public void setId(UUID id) { // asigna el id
        this.id = id; // establece id
    }

    public UUID getReservationId() { // obtiene la reserva
        return reservationId; // retorna reservationId
    }

    public void setReservationId(UUID reservationId) { // asigna la reserva
        this.reservationId = reservationId; // establece reservationId
    }

    public UUID getOwnerId() { // obtiene el propietario
        return ownerId; // retorna ownerId
    }

    public void setOwnerId(UUID ownerId) { // asigna el propietario
        this.ownerId = ownerId; // establece ownerId
    }

    public Instant getReportedAt() { // obtiene la hora del reporte
        return reportedAt; // retorna reportedAt
    }

    public void setReportedAt(Instant reportedAt) { // asigna la hora del reporte
        this.reportedAt = reportedAt; // establece reportedAt
    }

    public int getWaitingMinutes() { // obtiene los minutos esperados
        return waitingMinutes; // retorna waitingMinutes
    }

    public void setWaitingMinutes(int waitingMinutes) { // asigna los minutos esperados
        this.waitingMinutes = waitingMinutes; // establece waitingMinutes
    }

    public String getObservations() { // obtiene las observaciones
        return observations; // retorna observations
    }

    public void setObservations(String observations) { // asigna las observaciones
        this.observations = observations; // establece observations
    }
}