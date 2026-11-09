package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.util.UUID;

public record ChangeReservationStatusCommand( // comando de aplicacion para cambiar estado
        UUID reservationId, // identificador de la reserva
        ReservationStatus targetStatus, // estado de destino requerido
        CancellationSubtype subtype, // subtipo de cancelacion opcional
        ActorType actor, // actor que solicita la operacion
        String reason, // motivo o justificacion del cambio
        UUID actorId, // identificador del usuario; obligatorio si actor no es SYSTEM
        Long expectedVersion, // version vista por el invocador; opcional
        Boolean mechanicalFailure, // averia mecanica al cancelar; obligatorio si cancela OWNER
        Integer anticipationMinutes, // anticipacion cronologica; obligatorio si cancela RENTER
        String completionNotes, // notas de cierre; solo con target COMPLETED
        String paymentReference // referencia del cobro; solo con target RESERVED
) {
    public ChangeReservationStatusCommand( // constructor compacto de compatibilidad (TTL y tests existentes)
            UUID reservationId,
            ReservationStatus targetStatus,
            CancellationSubtype subtype,
            ActorType actor,
            String reason
    ) {
        this(reservationId, targetStatus, subtype, actor, reason, null, null, null, null, null, null); // delega con opcionales nulos
    }
}
