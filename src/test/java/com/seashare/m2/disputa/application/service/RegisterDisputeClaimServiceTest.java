package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterDisputeClaimServiceTest { // reglas del reclamo (CU-16) sin base de datos

    private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");

    private static final Instant WINDOW_END = START.plus(Duration.ofHours(24));

    private static final String DESCRIPTION = "El timon llego con holgura critica por impacto";

    @Mock
    private DisputeRepositoryPort repository; // persistencia simulada

    @Mock
    private ClockPort clockPort; // reloj fijo simulado

    private RegisterDisputeClaimService service; // caso de uso bajo prueba

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        service = new RegisterDisputeClaimService(repository, clockPort);
        ownerId = UUID.randomUUID();
    }

    private GuaranteeDispute pendingDispute() { // disputa recien abierta
        GuaranteeDispute dispute = GuaranteeDispute.open(UUID.randomUUID(), ownerId, START);
        when(repository.findById(dispute.id())).thenReturn(Optional.of(dispute));
        return dispute;
    }

    private void stubPersistence() { // solo los caminos exitosos escriben
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveClaim(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RegisterDisputeClaimCommand command(UUID disputeId, UUID claimant, String category) {
        return new RegisterDisputeClaimCommand(disputeId, claimant, DESCRIPTION, category, List.of("https://cdn/evidencia.jpg"));
    }

    @Test
    void registersClaimAndKeepsPending() { // ventana abierta
        GuaranteeDispute dispute = pendingDispute();
        stubPersistence();
        when(clockPort.now()).thenReturn(START.plus(Duration.ofHours(1)));

        DisputeClaimView view = service.register(command(dispute.id(), ownerId, "structure"));

        assertThat(view.status()).isEqualTo(DisputeStatus.PENDING);
        assertThat(view.autoCloseDisabled()).isTrue();
        assertThat(view.damageCategory()).isEqualTo("structure");
        assertThat(view.windowStart()).isEqualTo(START);
        assertThat(view.windowEnd()).isEqualTo(WINDOW_END);
        verify(repository).saveClaim(any(DisputeClaim.class));
        verify(repository).save(any(GuaranteeDispute.class));
    }

    @Test
    void acceptsClaimOneSecondBeforeWindowEnd() { // borde exacto inferior
        GuaranteeDispute dispute = pendingDispute();
        stubPersistence();
        when(clockPort.now()).thenReturn(WINDOW_END.minusSeconds(1));

        DisputeClaimView view = service.register(command(dispute.id(), ownerId, null));

        assertThat(view.status()).isEqualTo(DisputeStatus.PENDING);
    }

    @Test
    void rejectsClaimAtExactWindowEnd() { // now == window_end -> 409 WINDOW_EXPIRED
        GuaranteeDispute dispute = pendingDispute();
        when(clockPort.now()).thenReturn(WINDOW_END);

        assertThatThrownBy(() -> service.register(command(dispute.id(), ownerId, null)))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("WINDOW_EXPIRED"));
        verify(repository, never()).saveClaim(any());
    }

    @Test
    void rejectsSecondClaim() { // D-08: un solo reclamo, no editable
        GuaranteeDispute dispute = GuaranteeDispute.restore(UUID.randomUUID(), UUID.randomUUID(), ownerId,
                START, WINDOW_END, DisputeStatus.PENDING, true, null, null, null);
        when(repository.findById(dispute.id())).thenReturn(Optional.of(dispute));
        when(clockPort.now()).thenReturn(START.plus(Duration.ofHours(1)));

        assertThatThrownBy(() -> service.register(command(dispute.id(), ownerId, null)))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("CLAIM_ALREADY_EXISTS"));
    }

    @Test
    void rejectsClaimOnAcceptedDispute() { // estado final con reclamo resuelto
        GuaranteeDispute dispute = GuaranteeDispute.restore(UUID.randomUUID(), UUID.randomUUID(), ownerId,
                START, WINDOW_END, DisputeStatus.ACCEPTED, true, "procedente", UUID.randomUUID(), START.plus(Duration.ofHours(2)));
        when(repository.findById(dispute.id())).thenReturn(Optional.of(dispute));
        when(clockPort.now()).thenReturn(START.plus(Duration.ofHours(3)));

        assertThatThrownBy(() -> service.register(command(dispute.id(), ownerId, null)))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("DISPUTE_ALREADY_RESOLVED"));
    }

    @Test
    void rejectedByExpiryReturnsWindowExpired() { // cerrada por vencimiento sin reclamo
        GuaranteeDispute dispute = GuaranteeDispute.restore(UUID.randomUUID(), UUID.randomUUID(), ownerId,
                START, WINDOW_END, DisputeStatus.REJECTED, false, "sin reclamo en ventana", null, WINDOW_END);
        when(repository.findById(dispute.id())).thenReturn(Optional.of(dispute));
        when(clockPort.now()).thenReturn(WINDOW_END.plusSeconds(1));

        assertThatThrownBy(() -> service.register(command(dispute.id(), ownerId, null)))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("WINDOW_EXPIRED"));
    }

    @Test
    void rejectsClaimFromDifferentOwner() { // solo el propietario registrado
        GuaranteeDispute dispute = pendingDispute();
        UUID otherOwner = UUID.randomUUID();

        assertThatThrownBy(() -> service.register(command(dispute.id(), otherOwner, null)))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("RESERVATION_NOT_OWNED"));
    }

    @Test
    void unknownDisputeIsNotFound() { // 404 DISPUTE_NOT_FOUND
        UUID unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(command(unknown, ownerId, null)))
                .isInstanceOf(DisputeNotFoundException.class)
                .satisfies(e -> assertThat(((DisputeNotFoundException) e).getCode()).isEqualTo("DISPUTE_NOT_FOUND"));
    }

    @Test
    void rejectsUnknownCategory() { // categoria fuera del enum del contrato
        GuaranteeDispute dispute = pendingDispute();
        when(clockPort.now()).thenReturn(START.plus(Duration.ofHours(1)));

        assertThatThrownBy(() -> service.register(command(dispute.id(), ownerId, "rust")))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("INVALID_PARAMETERS"));
    }

    @Test
    void rejectsDescriptionShorterThanTenCharacters() { // contrato CU-16
        GuaranteeDispute dispute = pendingDispute();
        when(clockPort.now()).thenReturn(START.plus(Duration.ofHours(1)));

        assertThatThrownBy(() -> service.register(
                new RegisterDisputeClaimCommand(dispute.id(), ownerId, "corto", null, List.of())))
                .isInstanceOf(DisputeRuleException.class)
                .satisfies(e -> assertThat(((DisputeRuleException) e).getCode()).isEqualTo("INVALID_PARAMETERS"));
    }

    @Test
    void persistsClaimWithClaimantIdentity() { // el reclamo guarda el propietario autenticado
        GuaranteeDispute dispute = pendingDispute();
        stubPersistence();
        when(clockPort.now()).thenReturn(START.plus(Duration.ofHours(1)));

        service.register(command(dispute.id(), ownerId, "engine"));

        ArgumentCaptor<DisputeClaim> captor = ArgumentCaptor.forClass(DisputeClaim.class);
        verify(repository).saveClaim(captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo(ownerId);
        assertThat(captor.getValue().disputeId()).isEqualTo(dispute.id());
        assertThat(captor.getValue().registeredAt()).isEqualTo(START.plus(Duration.ofHours(1)));
    }
}
