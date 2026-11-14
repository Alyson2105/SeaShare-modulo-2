package com.seashare.m2.disputa.application.service;

import com.seashare.m2.disputa.application.dto.DisputeListView;
import com.seashare.m2.disputa.application.port.out.DisputeRepositoryPort;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeCategory;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.GuaranteeDispute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListDisputesServiceTest {

    private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");

    @Mock
    private DisputeRepositoryPort repository;

    private ListDisputesService service;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        service = new ListDisputesService(repository);
        ownerId = UUID.randomUUID();
    }

    @Test
    void adminDoesNotRestrictOwnerAndPreservesRepositoryOrder() {
        GuaranteeDispute older = GuaranteeDispute.open(UUID.randomUUID(), ownerId, START);
        GuaranteeDispute newer = GuaranteeDispute.open(UUID.randomUUID(), UUID.randomUUID(), START.plusSeconds(60));
        when(repository.findAll(null, null)).thenReturn(List.of(newer, older));
        when(repository.findClaimByDisputeId(newer.id())).thenReturn(Optional.empty());
        when(repository.findClaimByDisputeId(older.id())).thenReturn(Optional.empty());

        List<DisputeListView> result = service.list(UUID.randomUUID(), "ADMIN", null);

        assertThat(result).extracting(DisputeListView::disputeId).containsExactly(newer.id(), older.id());
        assertThat(result.getFirst().windowStart()).isEqualTo(newer.window().start());
        assertThat(result.getFirst().claim()).isNull();
        verify(repository).findAll(null, null);
    }

    @Test
    void ownerAndReservationFiltersAreAppliedTogetherAndCurrentClaimIsIncluded() {
        UUID reservationId = UUID.randomUUID();
        GuaranteeDispute dispute = GuaranteeDispute.open(reservationId, ownerId, START);
        DisputeClaim claim = DisputeClaim.register(dispute.id(), ownerId,
                "The vessel was returned with structural damage", DisputeCategory.STRUCTURE,
                List.of("https://cdn/evidencia-1.jpg"), START);
        when(repository.findAll(ownerId, reservationId)).thenReturn(List.of(dispute));
        when(repository.findClaimByDisputeId(dispute.id())).thenReturn(Optional.of(claim));

        List<DisputeListView> result = service.list(ownerId, "OWNER", reservationId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().claim()).isEqualTo(claim);
        assertThat(result.getFirst().reservationId()).isEqualTo(reservationId);
        verify(repository).findAll(ownerId, reservationId);
    }

    @Test
    void ownerWithoutReservationStillUsesOwnerFilter() {
        when(repository.findAll(ownerId, null)).thenReturn(List.of());

        assertThat(service.list(ownerId, "OWNER", null)).isEmpty();
        verify(repository).findAll(ownerId, null);
    }

    @Test
    void adminReservationFilterDoesNotRestrictOwner() {
        UUID reservationId = UUID.randomUUID();
        when(repository.findAll(null, reservationId)).thenReturn(List.of());

        assertThat(service.list(UUID.randomUUID(), "ADMIN", reservationId)).isEmpty();
        verify(repository).findAll(null, reservationId);
    }

    @Test
    void unauthorizedRolesAndMissingOwnerNeverReadRepository() {
        assertThatThrownBy(() -> service.list(ownerId, "RENTER", null))
                .isInstanceOf(DisputeRuleException.class);
        assertThatThrownBy(() -> service.list(null, "OWNER", null))
                .isInstanceOf(DisputeRuleException.class);
        assertThatThrownBy(() -> service.list(ownerId, null, null))
                .isInstanceOf(DisputeRuleException.class);
        verifyNoInteractions(repository);
    }
}
