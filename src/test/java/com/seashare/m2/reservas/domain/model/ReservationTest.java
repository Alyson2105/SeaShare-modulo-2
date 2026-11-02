package com.seashare.m2.reservas.domain.model;

import com.seashare.m2.reservas.domain.state.InvalidTransitionException;
import com.seashare.m2.reservas.domain.state.ReservationStateMachine;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest { // pruebas unitarias del modelo de dominio reservation

    private final ReservationStateMachine machine = new ReservationStateMachine(); // maquina de estados de reservas

    @Test
    void initiatedToCompletedThrowsInvalidTransition() { // valida que transicion directa no permitida lance excepcion
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .renterId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status(ReservationStatus.INITIATED)
                .cancellationSubtype(null)
                .version(0L)
                .build(); // instancia reserva inicial
        assertThatThrownBy(() -> reservation.changeStatus(ReservationStatus.COMPLETED, null, machine)) // intenta cambio invalido
                .isInstanceOf(InvalidTransitionException.class); // espera transicion invalida
    }

    @Test
    void cancelWithSubtypeStoresSubtype() { // valida cancelacion correcta almacenando subtipo
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .renterId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status(ReservationStatus.RESERVED)
                .cancellationSubtype(null)
                .version(0L)
                .build(); // instancia reserva reservada
        reservation.changeStatus(ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, machine); // ejecuta transicion a cancelado
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED); // verifica estado cancelado
        assertThat(reservation.getCancellationSubtype()).isEqualTo(CancellationSubtype.FLEXIBLE); // verifica preservacion del subtipo
    }
}
