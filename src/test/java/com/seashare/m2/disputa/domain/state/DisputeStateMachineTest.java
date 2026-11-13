package com.seashare.m2.disputa.domain.state;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisputeStateMachineTest { // matriz de transiciones de la disputa (CU-17 FR-001/FR-002/FR-003)

    private final DisputeStateMachine machine = new DisputeStateMachine();

    private void assertCode(Runnable action, String expectedCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((DisputeRuleException) e).getCode())
                        .isEqualTo(expectedCode));
    }

    @Test
    void pendingCanStayPendingRejectedOrAccepted() { // unicos destinos permitidos desde PENDING
        assertThatCode(() -> machine.validateTransition(DisputeStatus.PENDING, DisputeStatus.PENDING, false))
                .doesNotThrowAnyException();
        assertThatCode(() -> machine.validateTransition(DisputeStatus.PENDING, DisputeStatus.REJECTED, false))
                .doesNotThrowAnyException();
        assertThatCode(() -> machine.validateTransition(DisputeStatus.PENDING, DisputeStatus.ACCEPTED, true))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptedRequiresRegisteredClaim() { // D-6, [NEEDS CLARIFICATION] CU-17 D-01
        assertCode(() -> machine.validateTransition(DisputeStatus.PENDING, DisputeStatus.ACCEPTED, false),
                "INVALID_RESERVATION_STATE");
    }

    @Test
    void rejectedDoesNotRequireClaim() { // el cierre por vencimiento rechaza sin reclamo
        assertThatCode(() -> machine.validateTransition(DisputeStatus.PENDING, DisputeStatus.REJECTED, false))
                .doesNotThrowAnyException();
    }

    @Test
    void finalStatesCannotChange() { // FR-003
        assertCode(() -> machine.validateTransition(DisputeStatus.ACCEPTED, DisputeStatus.REJECTED, true),
                "DISPUTE_ALREADY_RESOLVED");
        assertCode(() -> machine.validateTransition(DisputeStatus.REJECTED, DisputeStatus.PENDING, false),
                "DISPUTE_ALREADY_RESOLVED");
        assertCode(() -> machine.validateTransition(DisputeStatus.REJECTED, DisputeStatus.REJECTED, false),
                "DISPUTE_ALREADY_RESOLVED");
    }

    @Test
    void nullTargetIsInvalid() { // destino obligatorio
        assertCode(() -> machine.validateTransition(DisputeStatus.PENDING, null, true), "INVALID_PARAMETERS");
    }
}
