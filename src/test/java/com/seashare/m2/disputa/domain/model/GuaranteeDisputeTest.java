package com.seashare.m2.disputa.domain.model;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuaranteeDisputeTest { // reglas basicas del agregado (CU-16)

    private static final Instant COMPLETED_AT = Instant.parse("2026-11-13T10:00:00Z");

    @Test
    void opensInPendingWith24HourWindow() { // apertura automatica
        GuaranteeDispute dispute = GuaranteeDispute.open(UUID.randomUUID(), UUID.randomUUID(), COMPLETED_AT);

        assertThat(dispute.status()).isEqualTo(DisputeStatus.PENDING);
        assertThat(dispute.hasClaim()).isFalse();
        assertThat(dispute.window().end()).isEqualTo(COMPLETED_AT.plus(Duration.ofHours(24)));
        assertThat(dispute.reason()).isNull();
        assertThat(dispute.resolvedAt()).isNull();
    }

    @Test
    void requiresReservationOwnerAndCompletionInstant() { // el contrato interno exige los tres datos
        assertThatThrownBy(() -> GuaranteeDispute.open(null, UUID.randomUUID(), COMPLETED_AT))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("INVALID_PARAMETERS"));

        assertThatThrownBy(() -> GuaranteeDispute.open(UUID.randomUUID(), null, COMPLETED_AT))
                .isInstanceOf(DisputeRuleException.class);

        assertThatThrownBy(() -> GuaranteeDispute.open(UUID.randomUUID(), UUID.randomUUID(), null))
                .isInstanceOf(DisputeRuleException.class);
    }

    @Test
    void registeringClaimDoesNotChangeStatus() { // CU-16 FR-006
        GuaranteeDispute dispute = GuaranteeDispute.open(UUID.randomUUID(), UUID.randomUUID(), COMPLETED_AT);

        dispute.registerClaim();

        assertThat(dispute.hasClaim()).isTrue();
        assertThat(dispute.status()).isEqualTo(DisputeStatus.PENDING);
    }

    @Test
    void finalStatusesDoNotAcceptClaims() { // REJECTED y ACCEPTED son terminales
        GuaranteeDispute rejected = GuaranteeDispute.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                COMPLETED_AT, COMPLETED_AT.plus(Duration.ofHours(24)), DisputeStatus.REJECTED,
                false, "sin reclamo en ventana", null, COMPLETED_AT.plus(Duration.ofHours(24)));

        assertThat(rejected.isFinal()).isTrue();
        assertThat(rejected.canRegisterClaim(COMPLETED_AT)).isFalse();
        assertThat(rejected.isExpired(COMPLETED_AT.plus(Duration.ofHours(24)))).isTrue();
    }
}
