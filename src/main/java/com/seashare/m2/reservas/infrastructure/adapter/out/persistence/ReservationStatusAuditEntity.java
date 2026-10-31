package com.seashare.m2.reservas.infrastructure.adapter.out.persistence;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
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
@Table(name = "reservation_status_audit")
@Immutable
public class ReservationStatusAuditEntity { // entidad jpa inmutable para auditoria de estados

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId; // referencia a la reserva auditada

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ReservationStatus fromStatus; // estado anterior

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, nullable = false)
    private ReservationStatus toStatus; // estado nuevo

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_subtype", length = 20)
    private CancellationSubtype cancellationSubtype; // subtipo de cancelacion

    @Column(name = "actor", length = 50, nullable = false)
    private String actor; // actor que origino el cambio

    @Column(name = "reason")
    private String reason; // motivo del cambio

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt; // marca temporal del cambio

    public ReservationStatusAuditEntity() { // constructor sin argumentos requerido por jpa
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public void setId(UUID id) { // asigna el id
        this.id = id; // establece id
    }

    public UUID getReservationId() { // obtiene el id de reserva
        return reservationId; // retorna reservationId
    }

    public void setReservationId(UUID reservationId) { // asigna el id de reserva
        this.reservationId = reservationId; // establece reservationId
    }

    public ReservationStatus getFromStatus() { // obtiene el estado anterior
        return fromStatus; // retorna fromStatus
    }

    public void setFromStatus(ReservationStatus fromStatus) { // asigna el estado anterior
        this.fromStatus = fromStatus; // establece fromStatus
    }

    public ReservationStatus getToStatus() { // obtiene el nuevo estado
        return toStatus; // retorna toStatus
    }

    public void setToStatus(ReservationStatus toStatus) { // asigna el nuevo estado
        this.toStatus = toStatus; // establece toStatus
    }

    public CancellationSubtype getCancellationSubtype() { // obtiene el subtipo de cancelacion
        return cancellationSubtype; // retorna cancellationSubtype
    }

    public void setCancellationSubtype(CancellationSubtype cancellationSubtype) { // asigna el subtipo de cancelacion
        this.cancellationSubtype = cancellationSubtype; // establece cancellationSubtype
    }

    public String getActor() { // obtiene el actor
        return actor; // retorna actor
    }

    public void setActor(String actor) { // asigna el actor
        this.actor = actor; // establece actor
    }

    public String getReason() { // obtiene el motivo
        return reason; // retorna reason
    }

    public void setReason(String reason) { // asigna el motivo
        this.reason = reason; // establece reason
    }

    public Instant getOccurredAt() { // obtiene la fecha y hora
        return occurredAt; // retorna occurredAt
    }

    public void setOccurredAt(Instant occurredAt) { // asigna la fecha y hora
        this.occurredAt = occurredAt; // establece occurredAt
    }
}
