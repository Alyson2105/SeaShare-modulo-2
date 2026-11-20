package com.seashare.m2.reservas.domain.state;

import com.seashare.m2.reservas.domain.exception.InvalidTransitionException;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStateMachineTest { // pruebas unitarias de la maquina de estados

    private final ReservationStateMachine stateMachine = new ReservationStateMachine(); // instancia bajo prueba

    @Test
    void happyPathIsAllowed() { // camino feliz completo de la reserva
        assertThat(stateMachine.canTransition(ReservationStatus.INITIATED, ReservationStatus.PENDING_PAYMENT)).isTrue(); // paso 1
        assertThat(stateMachine.canTransition(ReservationStatus.PENDING_PAYMENT, ReservationStatus.RESERVED)).isTrue(); // paso 2
        assertThat(stateMachine.canTransition(ReservationStatus.RESERVED, ReservationStatus.IN_NAVIGATION)).isTrue(); // paso 3
        assertThat(stateMachine.canTransition(ReservationStatus.IN_NAVIGATION, ReservationStatus.COMPLETED)).isTrue(); // paso 4
    }

    @Test
    void cannotSkipPayment() { // no permite saltar el pago
        assertThat(stateMachine.canTransition(ReservationStatus.INITIATED, ReservationStatus.RESERVED)).isFalse(); // transicion no permitida
        assertThatThrownBy(() -> stateMachine.validate(ReservationStatus.INITIATED, ReservationStatus.RESERVED, null)) // intento invalido
                .isInstanceOf(InvalidTransitionException.class) // tipo de excepcion
                .hasFieldOrPropertyWithValue("code", InvalidTransitionException.CODE); // codigo esperado
    }

    @Test
    void finalStatesHaveNoExit() { // estados finales no tienen salida
        for (ReservationStatus status : ReservationStatus.values()) { // itera todos los estados
            if (status.isFinal()) { // filtra por estados finales
                for (ReservationStatus target : ReservationStatus.values()) { // itera posibles destinos
                    assertThat(stateMachine.canTransition(status, target)).isFalse(); // ninguna transicion permitida
                }
            }
        }
    }

    @Test
    void cancelWithoutSubtypeFails() { // cancelacion sin subtipo falla
        assertThatThrownBy(() -> stateMachine.validate(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, null)) // falta subtipo
                .isInstanceOf(InvalidTransitionException.class); // lanza excepcion esperada
    }

    @Test
    void cancelWithSubtypeIsValid() { // cancelacion con subtipo es valida
        assertThatCode(() -> stateMachine.validate(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE)) // cancelacion correcta
                .doesNotThrowAnyException(); // no lanza ninguna excepcion
    }

    @Test
    void creationOnlyAcceptsInitiated() { // T-24: la creacion solo puede nacer en INITIATED
        assertThatCode(() -> stateMachine.validateCreation(ReservationStatus.INITIATED)) // unico destino valido
                .doesNotThrowAnyException(); // no lanza

        for (ReservationStatus target : ReservationStatus.values()) { // cualquier otro destino se rechaza
            if (target != ReservationStatus.INITIATED) {
                assertThatThrownBy(() -> stateMachine.validateCreation(target))
                        .isInstanceOf(InvalidTransitionException.class); // la creacion no salta etapas
            }
        }
    }

    @Test
    void genericCanTransitionStillRejectsNullOrigin() { // la creacion no abre la matriz generica
        assertThat(stateMachine.canTransition(null, ReservationStatus.INITIATED)).isFalse(); // null sigue sin transicion
        assertThatThrownBy(() -> stateMachine.validate(null, ReservationStatus.INITIATED, null))
                .isInstanceOf(InvalidTransitionException.class); // el execute generico no crea reservas
    }
}
