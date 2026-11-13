package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;
import com.seashare.m2.disputa.application.port.in.RegisterDisputeClaimUseCase;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisputeClaimControllerTest { // contrato HTTP de POST /api/v1/disputes/{id}/claims (CU-16)

    private static final Instant WINDOW_START = Instant.parse("2026-11-13T10:00:00Z");

    private static final Instant WINDOW_END = Instant.parse("2026-11-14T10:00:00Z");

    private static final Instant REGISTERED_AT = Instant.parse("2026-11-13T11:00:00Z");

    private static final String VALID_BODY = """
            {
              "description": "El timon llego con holgura critica por impacto",
              "damage_category": "structure",
              "evidence_urls": ["https://cdn/evidencia-1.jpg"]
            }
            """;

    private RegisterDisputeClaimUseCase useCase; // caso de uso simulado

    private MockMvc mockMvc; // cliente http de la capa web

    private UUID disputeId;

    private UUID ownerId;

    @BeforeEach
    void setUp() { // registra controlador, resolver propio y manejador global de errores
        useCase = mock(RegisterDisputeClaimUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DisputeClaimController(useCase))
                .setCustomArgumentResolvers(new DisputeCallerIdentityResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        disputeId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    private DisputeClaimView sampleView() { // respuesta esperada del reclamo
        return new DisputeClaimView(disputeId, UUID.randomUUID(), DisputeStatus.PENDING,
                UUID.randomUUID(), ownerId, "El timon llego con holgura critica por impacto",
                "structure", List.of("https://cdn/evidencia-1.jpg"), REGISTERED_AT, WINDOW_START, WINDOW_END, true);
    }

    @Test
    void createdReturns201WithContractFields() throws Exception { // FR-006, FR-015, FR-016
        when(useCase.register(any())).thenReturn(sampleView());

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dispute_id").value(disputeId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.claim.owner_id").value(ownerId.toString()))
                .andExpect(jsonPath("$.claim.damage_category").value("structure"))
                .andExpect(jsonPath("$.claim.evidence_urls[0]").value("https://cdn/evidencia-1.jpg"))
                .andExpect(jsonPath("$.claim.registered_at").value("2026-11-13T11:00:00Z"))
                .andExpect(jsonPath("$.auto_close_disabled").value(true))
                .andExpect(jsonPath("$.window_24h.start_at").value("2026-11-13T10:00:00Z"))
                .andExpect(jsonPath("$.window_24h.end_at").value("2026-11-14T10:00:00Z"))
                .andExpect(jsonPath("$.window_24h.window_status").value("CLOSED_BY_CLAIM_FILED"));

        ArgumentCaptor<RegisterDisputeClaimCommand> captor = ArgumentCaptor.forClass(RegisterDisputeClaimCommand.class);
        verify(useCase).register(captor.capture());
        assertThat(captor.getValue().disputeId()).isEqualTo(disputeId);
        assertThat(captor.getValue().claimantId()).isEqualTo(ownerId);
        assertThat(captor.getValue().damageCategory()).isEqualTo("structure");
    }

    @Test
    void nonOwnerRoleIs403() throws Exception { // D-11: rol distinto de OWNER
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "RENTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
    }

    @Test
    void missingIdentityIs401() throws Exception { // sin headers de identidad
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void invalidUuidIs400() throws Exception { // identificador no uuid en la ruta
        mockMvc.perform(post("/api/v1/disputes/not-a-uuid/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));
    }

    @Test
    void shortDescriptionIs400() throws Exception { // validacion de bean validation
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"corto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void windowExpiredIs409() throws Exception { // FR-009
        when(useCase.register(any())).thenThrow(new DisputeRuleException(ErrorCode.WINDOW_EXPIRED,
                "The 24 hour claim window has expired"));

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WINDOW_EXPIRED"));
    }

    @Test
    void duplicateClaimIs409() throws Exception { // D-08
        when(useCase.register(any())).thenThrow(new DisputeRuleException(ErrorCode.CLAIM_ALREADY_EXISTS,
                "A claim has already been registered for this dispute"));

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLAIM_ALREADY_EXISTS"));
    }

    @Test
    void notOwnerIs403() throws Exception { // D-11: propietario que no es el de la reserva
        when(useCase.register(any())).thenThrow(new DisputeRuleException(ErrorCode.RESERVATION_NOT_OWNED,
                "Only the registered owner of the reserved vessel may file the claim"));

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_OWNED"));
    }

    @Test
    void unknownDisputeIs404() throws Exception { // disputa inexistente
        when(useCase.register(any())).thenThrow(new DisputeNotFoundException(disputeId));

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/claims")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISPUTE_NOT_FOUND"));
    }
}
