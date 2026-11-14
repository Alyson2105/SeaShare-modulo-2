package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusResult;
import com.seashare.m2.disputa.application.port.out.DisputeOutboxPort;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.AdminReview;
import com.seashare.m2.disputa.domain.model.DisputeActorType;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.DisputeStatusChange;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDisputeStatusServiceTest { // transiciones de CU-17 sin base de datos

    private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");

    private static final Instant WINDOW_END = START.plus(Duration.ofHours(24));

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    @Mock
    private DisputeRepositoryPort repository;

    @Mock
    private DisputeOutboxPort disputeOutboxPort;

    @Mock
    private ClockPort clockPort;

    private UpdateDisputeStatusService service;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        service = new UpdateDisputeStatusService(repository, disputeOutboxPort, clockPort);
        adminId = UUID.randomUUID();
    }

    private GuaranteeDispute dispute(DisputeStatus status, boolean hasClaim) { // disputa en el estado de partida
        GuaranteeDispute dispute = GuaranteeDispute.restore(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                START, WINDOW_END, status, hasClaim, null, null, null);
        when(repository.findById(dispute.id())).thenReturn(Optional.of(dispute));
        return dispute;
    }

    private void stubWrites() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UpdateDisputeStatusCommand command(UUID disputeId, DisputeStatus target, DisputeActorType actor,
                                               UUID reviewer, String reason) {
        return new UpdateDisputeStatusCommand(disputeId, target, reviewer, reason, "nota interna", actor);
    }

    @Test
    void adminAcceptsDisputeWithClaim() { // escenario 1 de CU-17
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, true);
        stubWrites();
        when(clockPort.now()).thenReturn(NOW);

        UpdateDisputeStatusResult result = service.update(
                command(dispute.id(), DisputeStatus.ACCEPTED, DisputeActorType.ADMIN, adminId, "procedente"));

        assertThat(result.previousStatus()).isEqualTo(DisputeStatus.PENDING);
        assertThat(result.newStatus()).isEqualTo(DisputeStatus.ACCEPTED);
        assertThat(result.resolvedBy()).isEqualTo(adminId);
        assertThat(result.resolvedAt()).isEqualTo(NOW);
        assertThat(result.m3EventPublished()).isTrue();

        ArgumentCaptor<AdminReview> review = ArgumentCaptor.forClass(AdminReview.class);
        verify(repository).saveAdminReview(review.capture());
        assertThat(review.getValue().decision()).isEqualTo(DisputeStatus.ACCEPTED);
        assertThat(review.getValue().adminId()).isEqualTo(adminId);

        ArgumentCaptor<DisputeStatusChange> change = ArgumentCaptor.forClass(DisputeStatusChange.class);
        verify(repository).recordStatusChange(change.capture());
        assertThat(change.getValue().fromStatus()).isEqualTo(DisputeStatus.PENDING);
        assertThat(change.getValue().toStatus()).isEqualTo(DisputeStatus.ACCEPTED);
        assertThat(change.getValue().actorType()).isEqualTo(DisputeActorType.ADMIN);

        ArgumentCaptor<com.seashare.m2.disputa.application.dto.DisputeUpdatedEvent> event =
                ArgumentCaptor.forClass(com.seashare.m2.disputa.application.dto.DisputeUpdatedEvent.class);
        verify(disputeOutboxPort).enqueue(event.capture()); // CU-18: evento en la misma transaccion
        assertThat(event.getValue().financeStatus()).isEqualTo("COMPLETED");
        assertThat(event.getValue().eventKey()).isEqualTo("disputa-" + dispute.id() + "-COMPLETED");
        assertThat(event.getValue().statusChangedAt()).isEqualTo(NOW);
    }

    @Test
    void adminRejectsWithReason() { // escenario 2 de CU-17: motivo local
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, true);
        stubWrites();
        when(clockPort.now()).thenReturn(NOW);

        UpdateDisputeStatusResult result = service.update(
                command(dispute.id(), DisputeStatus.REJECTED, DisputeActorType.ADMIN, adminId, "desgaste normal"));

        assertThat(result.newStatus()).isEqualTo(DisputeStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("desgaste normal");
        verify(repository).saveAdminReview(any());
    }

    @Test
    void adminKeepsPendingWithoutTransition() { // revision en curso: sin cambio efectivo
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, true);
        when(clockPort.now()).thenReturn(NOW);

        UpdateDisputeStatusResult result = service.update(
                command(dispute.id(), DisputeStatus.PENDING, DisputeActorType.ADMIN, adminId, null));

        assertThat(result.newStatus()).isEqualTo(DisputeStatus.PENDING);
        assertThat(result.m3EventPublished()).isFalse();
        verify(repository).saveAdminReview(any());
        verify(repository, never()).save(any());
        verify(repository, never()).recordStatusChange(any());
        verify(disputeOutboxPort, never()).enqueue(any()); // PENDING nunca genera mensaje
    }

    @Test
    void adminCannotAcceptWithoutClaim() { // D-6: 409 con el codigo existente mas cercano
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, false);
        when(clockPort.now()).thenReturn(NOW);

        assertThatThrownBy(() -> service.update(
                command(dispute.id(), DisputeStatus.ACCEPTED, DisputeActorType.ADMIN, adminId, "sin reclamo")))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("INVALID_RESERVATION_STATE"));
        verify(repository, never()).saveAdminReview(any());
    }

    @Test
    void adminCannotRejectWithoutClaim() { // decision de negocio: solo el sistema cierra sin reclamo
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, false);
        when(clockPort.now()).thenReturn(NOW);

        assertThatThrownBy(() -> service.update(
                command(dispute.id(), DisputeStatus.REJECTED, DisputeActorType.ADMIN, adminId, "sin reclamo")))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("INVALID_RESERVATION_STATE"));
        verify(repository, never()).saveAdminReview(any());
        verify(repository, never()).save(any());
        verify(disputeOutboxPort, never()).enqueue(any());
    }

    @Test
    void finalDisputeCannotBeChanged() { // FR-003
        GuaranteeDispute dispute = dispute(DisputeStatus.ACCEPTED, true);
        when(clockPort.now()).thenReturn(NOW);

        assertThatThrownBy(() -> service.update(
                command(dispute.id(), DisputeStatus.REJECTED, DisputeActorType.ADMIN, adminId, "reapertura")))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("DISPUTE_ALREADY_RESOLVED"));
        verify(repository, never()).save(any());
    }

    @Test
    void systemCloseRejectsWithoutClaimAndWithoutAdminReview() { // CU-16 FR-008
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, false);
        stubWrites();
        when(clockPort.now()).thenReturn(WINDOW_END);

        UpdateDisputeStatusResult result = service.update(new UpdateDisputeStatusCommand(
                dispute.id(), DisputeStatus.REJECTED, null,
                GuaranteeDispute.SYSTEM_CLOSE_REASON, null, DisputeActorType.SYSTEM));

        assertThat(result.newStatus()).isEqualTo(DisputeStatus.REJECTED);
        assertThat(result.resolvedBy()).isNull();
        assertThat(result.reason()).isEqualTo(GuaranteeDispute.SYSTEM_CLOSE_REASON);
        verify(repository, never()).saveAdminReview(any());
        verify(disputeOutboxPort).enqueue(any()); // REJECTED se publica como REJECTED

        ArgumentCaptor<DisputeStatusChange> change = ArgumentCaptor.forClass(DisputeStatusChange.class);
        verify(repository).recordStatusChange(change.capture());
        assertThat(change.getValue().actorType()).isEqualTo(DisputeActorType.SYSTEM);
        assertThat(change.getValue().actorId()).isNull();
    }

    @Test
    void optimisticLockBecomesAlreadyResolved() { // carrera Admin vs cierre automatico
        GuaranteeDispute dispute = dispute(DisputeStatus.PENDING, true);
        when(clockPort.now()).thenReturn(NOW);
        when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("version"));

        assertThatThrownBy(() -> service.update(
                command(dispute.id(), DisputeStatus.ACCEPTED, DisputeActorType.ADMIN, adminId, "procedente")))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("DISPUTE_ALREADY_RESOLVED"));
    }

    @Test
    void unknownDisputeIsNotFound() { // 404
        UUID unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                command(unknown, DisputeStatus.REJECTED, DisputeActorType.ADMIN, adminId, null)))
                .isInstanceOf(DisputeNotFoundException.class);
    }
}
