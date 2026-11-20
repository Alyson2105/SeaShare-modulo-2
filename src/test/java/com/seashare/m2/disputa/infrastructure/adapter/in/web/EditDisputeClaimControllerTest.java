package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.port.in.EditDisputeClaimUseCase;
import com.seashare.m2.disputa.domain.model.DisputeStatus;
import com.seashare.m2.reservas.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EditDisputeClaimControllerTest {
    @Test
    void ownerCanSubmitAnEditedClaim() throws Exception {
        var useCase = mock(EditDisputeClaimUseCase.class);
        UUID dispute = UUID.randomUUID(), owner = UUID.randomUUID(), claim = UUID.randomUUID();
        Instant start = Instant.parse("2026-11-13T10:00:00Z");
        when(useCase.edit(any())).thenReturn(new DisputeClaimView(dispute, UUID.randomUUID(), DisputeStatus.PENDING,
                claim, owner, "Descripcion modificada", "other", List.of(), start, start, start.plusSeconds(86400), true));
        var mvc = MockMvcBuilders.standaloneSetup(new EditDisputeClaimController(useCase))
                .setCustomArgumentResolvers(new DisputeCallerIdentityResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(put("/api/v1/disputes/{id}/claims", dispute).header("X-User-Id", owner.toString())
                        .header("X-User-Role", "OWNER").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Descripcion modificada\",\"damage_category\":\"other\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.claim.claim_id").value(claim.toString()))
                .andExpect(jsonPath("$.window_24h.window_status").value("OPEN_FOR_EDIT"));
        verify(useCase).edit(any());
    }
}
