package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeUpdatedEvent;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisputeEventMapperTest { // traduccion M2 -> M3 y clave de evento (CU-18)

    private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    private GuaranteeDispute dispute(DisputeStatus status) { // disputa en el estado indicado
        return GuaranteeDispute.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                START, START.plus(Duration.ofHours(24)), status, true, null, null, null);
    }

    @Test
    void acceptedIsPublishedAsCompleted() { // tabla de mapeo de CU-18
        GuaranteeDispute dispute = dispute(DisputeStatus.ACCEPTED);

        DisputeUpdatedEvent event = DisputeEventMapper.finalEvent(dispute, NOW);

        assertThat(event.financeStatus()).isEqualTo("COMPLETED");
        assertThat(event.eventKey()).isEqualTo("disputa-" + dispute.id() + "-COMPLETED");
        assertThat(event.reservationId()).isEqualTo(dispute.reservationId());
        assertThat(event.disputeId()).isEqualTo(dispute.id());
        assertThat(event.statusChangedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectedIsPublishedAsRejected() { // REJECTED -> REJECTED (D-3)
        GuaranteeDispute dispute = dispute(DisputeStatus.REJECTED);

        DisputeUpdatedEvent event = DisputeEventMapper.finalEvent(dispute, NOW);

        assertThat(event.financeStatus()).isEqualTo("REJECTED");
        assertThat(event.eventKey()).isEqualTo("disputa-" + dispute.id() + "-REJECTED");
    }

    @Test
    void eventIdIsDeterministicPerDisputeAndStatus() { // FR-004: deduplicacion del lado de M3
        GuaranteeDispute dispute = dispute(DisputeStatus.ACCEPTED);

        DisputeUpdatedEvent first = DisputeEventMapper.finalEvent(dispute, NOW);
        DisputeUpdatedEvent second = DisputeEventMapper.finalEvent(dispute, NOW.plusSeconds(30));

        UUID expected = UUID.nameUUIDFromBytes(
                ("disputa-" + dispute.id() + "-COMPLETED").getBytes(StandardCharsets.UTF_8));
        assertThat(first.eventId()).isEqualTo(expected);
        assertThat(second.eventId()).isEqualTo(expected);
    }

    @Test
    void pendingNeverProducesAnEvent() { // FR-001: PENDING es interno
        GuaranteeDispute dispute = GuaranteeDispute.open(UUID.randomUUID(), UUID.randomUUID(), START);

        assertThatThrownBy(() -> DisputeEventMapper.finalEvent(dispute, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
