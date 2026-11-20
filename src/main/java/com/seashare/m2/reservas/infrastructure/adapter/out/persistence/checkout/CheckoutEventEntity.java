package com.seashare.m2.reservas.infrastructure.adapter.out.persistence.checkout;

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
@Table(name = "checkout_event")
@Immutable
class CheckoutEventEntity { // entidad jpa inmutable para el evento de checkout

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // clave primaria uuid

    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId; // reserva que termino la navegacion

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; // propietario que registro el cierre

    @Column(name = "actual_time", nullable = false)
    private Instant actualTime; // instante real de llegada

    @Column(name = "remarks")
    private String remarks; // novedades del cierre

    public CheckoutEventEntity() { // constructor sin argumentos requerido por jpa
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

    public Instant getActualTime() { // obtiene la hora real
        return actualTime; // retorna actualTime
    }

    public void setActualTime(Instant actualTime) { // asigna la hora real
        this.actualTime = actualTime; // establece actualTime
    }

    public String getRemarks() { // obtiene las novedades
        return remarks; // retorna remarks
    }

    public void setRemarks(String remarks) { // asigna las novedades
        this.remarks = remarks; // establece remarks
    }
}