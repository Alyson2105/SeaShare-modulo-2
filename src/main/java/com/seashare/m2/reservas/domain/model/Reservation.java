package com.seashare.m2.reservas.domain.model;

import com.seashare.m2.reservas.domain.state.ReservationStateMachine;

import java.util.UUID;

public class Reservation { // agregado de reserva del dominio

    private final UUID id; // identificador unico de la reserva
    private final UUID renterId; // identificador del arrendatario
    private final UUID ownerId; // identificador del propietario
    private ReservationStatus status; // estado actual de la reserva
    private CancellationSubtype cancellationSubtype; // subtipo de cancelacion de la reserva
    private final Long version; // version de concurrencia optimista

    public Reservation(UUID id, UUID renterId, UUID ownerId, ReservationStatus status, CancellationSubtype cancellationSubtype, Long version) { // constructor completo del agregado
        this.id = id; // asigna identificador
        this.renterId = renterId; // asigna arrendatario
        this.ownerId = ownerId; // asigna propietario
        this.status = status; // asigna estado
        this.cancellationSubtype = cancellationSubtype; // asigna subtipo
        this.version = version; // asigna version
    }

    public void changeStatus(ReservationStatus to, CancellationSubtype subtype, ReservationStateMachine machine) { // cambia el estado validando reglas
        machine.validate(this.status, to, subtype); // valida transicion segun maquina de estados
        this.status = to; // asigna nuevo estado
        this.cancellationSubtype = (to == ReservationStatus.CANCELLED ? subtype : null); // asigna subtipo solo si el estado es cancelado
    }

    public UUID getId() { // obtiene el id
        return id; // retorna id
    }

    public UUID getRenterId() { // obtiene el id del arrendatario
        return renterId; // retorna renterId
    }

    public UUID getOwnerId() { // obtiene el id del propietario
        return ownerId; // retorna ownerId
    }

    public ReservationStatus getStatus() { // obtiene el estado
        return status; // retorna status
    }

    public CancellationSubtype getCancellationSubtype() { // obtiene el subtipo de cancelacion
        return cancellationSubtype; // retorna cancellationSubtype
    }

    public Long getVersion() { // obtiene la version
        return version; // retorna version
    }
}
