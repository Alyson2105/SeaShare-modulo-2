package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.DisputeListView;
import com.seashare.m2.disputa.application.port.in.ListDisputesUseCase;
import com.seashare.m2.disputa.domain.model.DisputeCategory;
import com.seashare.m2.disputa.domain.model.DisputeClaim;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.reservas.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisputeQueryControllerTest {

    private static final Instant START = Instant.parse("2026-11-13T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant REGISTERED_AT = Instant.parse("2026-11-13T11:00:00Z");

    private ListDisputesUseCase useCase;
    private MockMvc mockMvc;
    private UUID adminId;
    private UUID ownerId;
    private UUID reservationId;
    private UUID disputeId;

    @BeforeEach
    void setUp() {
        useCase = mock(ListDisputesUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DisputeQueryController(useCase))
                .setCustomArgumentResolvers(new DisputeCallerIdentityResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        adminId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        disputeId = UUID.randomUUID();
    }

    private DisputeListView withClaim() {
        DisputeClaim claim = new DisputeClaim(UUID.randomUUID(), disputeId, ownerId,
                "The vessel was returned with structural damage", DisputeCategory.STRUCTURE,
                List.of("https://cdn/evidencia-1.jpg"), REGISTERED_AT);
        return new DisputeListView(disputeId, reservationId, DisputeStatus.PENDING, START, END, claim);
    }

    @Test
    void adminReceivesAllDisputesWithContractFields() throws Exception {
        DisputeListView first = withClaim();
        DisputeListView second = new DisputeListView(UUID.randomUUID(), UUID.randomUUID(),
                DisputeStatus.REJECTED, START.minusSeconds(60), END.minusSeconds(60), null);
        when(useCase.list(adminId, "ADMIN", null)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/disputes")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disputes.length()").value(2))
                .andExpect(jsonPath("$.disputes[0].dispute_id").value(disputeId.toString()))
                .andExpect(jsonPath("$.disputes[0].reservation_id").value(reservationId.toString()))
                .andExpect(jsonPath("$.disputes[0].status").value("PENDING"))
                .andExpect(jsonPath("$.disputes[0].window_start").value(START.toString()))
                .andExpect(jsonPath("$.disputes[0].window_end").value(END.toString()))
                .andExpect(jsonPath("$.disputes[0].claim.claim_id").value(first.claim().id().toString()))
                .andExpect(jsonPath("$.disputes[0].claim.owner_id").value(ownerId.toString()))
                .andExpect(jsonPath("$.disputes[0].claim.description").value(first.claim().description()))
                .andExpect(jsonPath("$.disputes[0].claim.damage_category").value("structure"))
                .andExpect(jsonPath("$.disputes[0].claim.evidence_urls[0]").value("https://cdn/evidencia-1.jpg"))
                .andExpect(jsonPath("$.disputes[0].claim.registered_at").value(REGISTERED_AT.toString()))
                .andExpect(jsonPath("$.disputes[1].dispute_id").value(second.disputeId().toString()))
                .andExpect(jsonPath("$.disputes[1].claim").doesNotExist());

        verify(useCase).list(adminId, "ADMIN", null);
    }

    @Test
    void ownerReceivesOnlyOwnDisputes() throws Exception {
        when(useCase.list(ownerId, "OWNER", null)).thenReturn(List.of(withClaim()));

        mockMvc.perform(get("/api/v1/disputes")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disputes.length()").value(1))
                .andExpect(jsonPath("$.disputes[0].claim.owner_id").value(ownerId.toString()));

        verify(useCase).list(ownerId, "OWNER", null);
    }

    @Test
    void reservationFilterIsPassedForBothRoles() throws Exception {
        for (String role : List.of("ADMIN", "OWNER")) {
            UUID userId = "ADMIN".equals(role) ? adminId : ownerId;
            when(useCase.list(userId, role, reservationId)).thenReturn(List.of(withClaim()));

            mockMvc.perform(get("/api/v1/disputes")
                            .param("reservation_id", reservationId.toString())
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", role))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disputes.length()").value(1))
                    .andExpect(jsonPath("$.disputes[0].reservation_id").value(reservationId.toString()));

            verify(useCase).list(userId, role, reservationId);
        }
    }

    @Test
    void renterRoleIsForbiddenWithoutCallingUseCase() throws Exception {
        mockMvc.perform(get("/api/v1/disputes")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));

        verifyNoInteractions(useCase);
    }

    @Test
    void invalidReservationUuidIsBadRequestWithoutCallingUseCase() throws Exception {
        for (String value : List.of("not-a-uuid", "", "1-1-1-1-1")) {
            mockMvc.perform(get("/api/v1/disputes")
                            .param("reservation_id", value)
                            .header("X-User-Id", adminId.toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_UUID"));
        }

        verifyNoInteractions(useCase);
    }

    @Test
    void emptyListReturnsEmptyDisputes() throws Exception {
        when(useCase.list(adminId, "ADMIN", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/disputes")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disputes").isArray())
                .andExpect(jsonPath("$.disputes").isEmpty());
    }

    @Test
    void missingIdentityIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/disputes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(useCase);
    }
}
