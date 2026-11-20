package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusResult;
import com.seashare.m2.disputa.application.port.in.UpdateDisputeStatusUseCase;
import com.seashare.m2.disputa.domain.exception.DisputeNotFoundException;
import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.reservas.infrastructure.adapter.in.web.GlobalExceptionHandler;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisputeAdminControllerTest { // contrato HTTP de PUT /api/v1/disputes/{id}/status (CU-17)

    private static final Instant RESOLVED_AT = Instant.parse("2026-11-13T16:45:00Z");

    private static final String BODY = """
            {
              "new_status": "ACCEPTED",
              "reason": "Evidencia fotografica incompatible con el desgaste normal",
              "internal_notes": "Inspeccion validada"
            }
            """;

    private UpdateDisputeStatusUseCase useCase; // caso de uso simulado

    private MockMvc mockMvc; // capa web aislada

    private UUID disputeId;

    private UUID reservationId;

    private UUID adminId;

    @BeforeEach
    void setUp() {
        useCase = mock(UpdateDisputeStatusUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DisputeAdminController(useCase))
                .setCustomArgumentResolvers(new DisputeCallerIdentityResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        disputeId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    private UpdateDisputeStatusResult accepted() { // resultado de una aceptacion
        return new UpdateDisputeStatusResult(disputeId, reservationId, DisputeStatus.PENDING,
                DisputeStatus.ACCEPTED, adminId, RESOLVED_AT, "Evidencia fotografica", true);
    }

    @Test
    void acceptedReturns200WithContractFields() throws Exception { // escenario 1 de CU-17
        when(useCase.update(any())).thenReturn(accepted());

        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispute_id").value(disputeId.toString()))
                .andExpect(jsonPath("$.reservation_id").value(reservationId.toString()))
                .andExpect(jsonPath("$.previous_status").value("PENDING"))
                .andExpect(jsonPath("$.new_status").value("ACCEPTED"))
                .andExpect(jsonPath("$.resolved_by").value(adminId.toString()))
                .andExpect(jsonPath("$.resolved_at").value("2026-11-13T16:45:00Z"))
                .andExpect(jsonPath("$.m3_event_published").value(true));

        ArgumentCaptor<com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand> captor =
                ArgumentCaptor.forClass(com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand.class);
        verify(useCase).update(captor.capture());
        assertThat(captor.getValue().targetStatus()).isEqualTo(DisputeStatus.ACCEPTED);
        assertThat(captor.getValue().adminId()).isEqualTo(adminId);
        assertThat(captor.getValue().actorType().name()).isEqualTo("ADMIN");
    }

    @Test
    void pendingReturns200WithoutEvent() throws Exception { // revision en curso: sin evento a M3
        when(useCase.update(any())).thenReturn(new UpdateDisputeStatusResult(disputeId, reservationId,
                DisputeStatus.PENDING, DisputeStatus.PENDING, adminId, RESOLVED_AT, null, false));

        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"new_status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.new_status").value("PENDING"))
                .andExpect(jsonPath("$.m3_event_published").value(false));
    }

    @Test
    void nonAdminRoleIs403() throws Exception { // solo el rol ADMIN
        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
    }

    @Test
    void missingIdentityIs401() throws Exception {
        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void invalidStatusIs400() throws Exception { // destino fuera del catalogo
        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"new_status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void invalidUuidIs400() throws Exception {
        mockMvc.perform(put("/api/v1/disputes/not-a-uuid/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));
    }

    @Test
    void alreadyResolvedIs409() throws Exception {
        when(useCase.update(any())).thenThrow(new DisputeRuleException(ErrorCode.DISPUTE_ALREADY_RESOLVED,
                "The dispute has already reached a final state"));

        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DISPUTE_ALREADY_RESOLVED"));
    }

    @Test
    void acceptWithoutClaimIs409() throws Exception { // D-6
        when(useCase.update(any())).thenThrow(new DisputeRuleException(ErrorCode.INVALID_RESERVATION_STATE,
                "A dispute can only be ACCEPTED when the owner has registered a claim"));

        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
    }

    @Test
    void unknownDisputeIs404() throws Exception {
        when(useCase.update(any())).thenThrow(new DisputeNotFoundException(disputeId));

        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/status")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISPUTE_NOT_FOUND"));
    }
}
