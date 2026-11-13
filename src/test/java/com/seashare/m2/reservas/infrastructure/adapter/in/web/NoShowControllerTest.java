package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.ReportNoShowCommand;
import com.seashare.m2.reservas.application.dto.ReportNoShowResult;
import com.seashare.m2.reservas.application.port.in.ReportNoShowUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
class NoShowControllerTest { // CU-05: contrato HTTP con caso de uso mockeado

    private static final UUID RESERVATION_ID = UUID.fromString("a1111111-2222-3333-4444-555555555555"); // reserva de prueba

    private static final UUID USER_ID = UUID.fromString("b1111111-2222-3333-4444-555555555555"); // usuario de prueba

    private static final UUID VESSEL_ID = UUID.fromString("c1111111-2222-3333-4444-555555555555"); // embarcacion de prueba

    private static final OffsetDateTime CANCELLED_AT = OffsetDateTime.parse("2027-04-10T09:30:00-05:00"); // cancelacion con offset

    private static final String MESSAGE = "Inasistencia confirmada. La reserva ha sido cancelada y la embarcacion quedo Disponible. La compensacion al propietario sera gestionada por el sistema de pagos."; // mensaje con liberacion

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc

    @MockitoBean
    private ReportNoShowUseCase reportNoShowUseCase; // caso de uso reemplazado

    private ReportNoShowResult result() { // resultado exitoso simulado
        return new ReportNoShowResult(RESERVATION_ID, ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW,
                CANCELLED_AT, 30, VESSEL_ID, OperationalStatus.AVAILABLE, MESSAGE);
    }

    @Test
    void withoutBodyReturnsContractFields() throws Exception { // 200 sin body
        when(reportNoShowUseCase.execute(any())).thenReturn(result());

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation_id").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellation_subtype").value("NO_SHOW"))
                .andExpect(jsonPath("$.cancelled_at").value("2027-04-10T09:30:00-05:00"))
                .andExpect(jsonPath("$.wait_minutes_recorded").value(30))
                .andExpect(jsonPath("$.vessel_id").value(VESSEL_ID.toString()))
                .andExpect(jsonPath("$.vessel_operational_status").value("AVAILABLE"))
                .andExpect(jsonPath("$.message").value(MESSAGE));
    }

    @Test
    void withBodyMapsObservations() throws Exception { // 200 con body
        when(reportNoShowUseCase.execute(any())).thenReturn(result());
        String body = """
                {
                  "observations": "No se presento en el muelle"
                }
                """;

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<ReportNoShowCommand> captor = ArgumentCaptor.forClass(ReportNoShowCommand.class);
        verify(reportNoShowUseCase).execute(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(captor.getValue().callerId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().actor()).isEqualTo(ActorType.OWNER);
        assertThat(captor.getValue().observations()).isEqualTo("No se presento en el muelle");
    }

    @Test
    void invalidPathUuidIs400() throws Exception { // uuid invalido en ruta
        mockMvc.perform(post("/api/v1/reservations/not-a-uuid/no-show")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));

        verify(reportNoShowUseCase, never()).execute(any());
    }

    @Test
    void malformedJsonIs400() throws Exception { // json ilegible
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(reportNoShowUseCase, never()).execute(any());
    }

    @Test
    void missingIdentityHeadersAre401() throws Exception { // sin headers de identidad
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(reportNoShowUseCase, never()).execute(any());
    }

    @Test
    void useCaseErrorsKeepTheirHttpCodes() throws Exception { // 403, 404 y tres 409
        when(reportNoShowUseCase.execute(any()))
                .thenThrow(new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "Not owner"))
                .thenThrow(new ReservationNotFoundException(RESERVATION_ID))
                .thenThrow(new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "Invalid state"))
                .thenThrow(new ReservationRuleException(ErrorCode.TRIP_NOT_STARTED, "Trip not started"))
                .thenThrow(new ReservationRuleException(ErrorCode.GRACE_PERIOD_NOT_ELAPSED, "Grace period active"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_STARTED"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/no-show", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRACE_PERIOD_NOT_ELAPSED"));
    }
}