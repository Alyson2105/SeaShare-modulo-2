package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.cancellation;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cancellation_event")
@Immutable
class CancellationEventEntity { // entidad jpa inmutable para el evento de cancelacion

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId; // reserva cancelada

    @Column(name = "actor", length = 50, nullable = false)
    private String actor; // actor que cancela

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt; // instante de la solicitud

    @Column(name = "anticipation_minutes", nullable = false)
    private long anticipationMinutes; // minutos de anticipacion; 0 para OWNER

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_subtype", length = 20)
    private CancellationSubtype cancellationSubtype; // clasificacion aplicada

    @Column(name = "reason")
    private String reason; // motivo del propietario

    @Column(name = "justification")
    private String justification; // justificacion libre

    public CancellationEventEntity() { // constructor sin argumentos requerido por jpa
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

    public String getActor() { // obtiene el actor
        return actor; // retorna actor
    }

    public void setActor(String actor) { // asigna el actor
        this.actor = actor; // establece actor
    }

    public Instant getRequestedAt() { // obtiene el instante de la solicitud
        return requestedAt; // retorna requestedAt
    }

    public void setRequestedAt(Instant requestedAt) { // asigna el instante de la solicitud
        this.requestedAt = requestedAt; // establece requestedAt
    }

    public long getAnticipationMinutes() { // obtiene los minutos de anticipacion
        return anticipationMinutes; // retorna anticipationMinutes
    }

    public void setAnticipationMinutes(long anticipationMinutes) { // asigna los minutos de anticipacion
        this.anticipationMinutes = anticipationMinutes; // establece anticipationMinutes
    }

    public CancellationSubtype getCancellationSubtype() { // obtiene el subtipo
        return cancellationSubtype; // retorna cancellationSubtype
    }

    public void setCancellationSubtype(CancellationSubtype cancellationSubtype) { // asigna el subtipo
        this.cancellationSubtype = cancellationSubtype; // establece cancellationSubtype
    }

    public String getReason() { // obtiene el motivo
        return reason; // retorna reason
    }

    public void setReason(String reason) { // asigna el motivo
        this.reason = reason; // establece reason
    }

    public String getJustification() { // obtiene la justificacion
        return justification; // retorna justification
    }

    public void setJustification(String justification) { // asigna la justificacion
        this.justification = justification; // establece justification
    }
}