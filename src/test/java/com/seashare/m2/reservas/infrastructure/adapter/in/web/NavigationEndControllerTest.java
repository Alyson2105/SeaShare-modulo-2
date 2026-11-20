package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.EndNavigationCommand;
import com.seashare.m2.reservas.application.dto.EndNavigationResult;
import com.seashare.m2.reservas.application.port.in.EndNavigationUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
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
import static org.hamcrest.Matchers.nullValue;
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
class NavigationEndControllerTest { // CU-07: contrato HTTP con caso de uso mockeado

    private static final UUID RESERVATION_ID = UUID.fromString("a1111111-2222-3333-4444-555555555555"); // reserva de prueba

    private static final UUID USER_ID = UUID.fromString("b1111111-2222-3333-4444-555555555555"); // usuario de prueba

    private static final UUID VESSEL_ID = UUID.fromString("c1111111-2222-3333-4444-555555555555"); // embarcacion de prueba

    private static final OffsetDateTime ARRIVAL = OffsetDateTime.parse("2027-04-10T17:30:00-05:00"); // llegada con offset

    private static final String MESSAGE_WITH_M1 = "Fin de navegacion registrado exitosamente. La reserva ha pasado a Completada y se encolo la senal para que Modulo 1 pase la embarcacion a En Mantenimiento/Limpieza."; // mensaje con aviso a M1

    private static final String MESSAGE_WITH_REMARKS = "Fin de navegacion registrado con novedades. La reserva ha pasado a Completada y se encolo la senal para que Modulo 1 pase la embarcacion a En Mantenimiento/Limpieza."; // mensaje con novedades

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc

    @MockitoBean
    private EndNavigationUseCase endNavigationUseCase; // caso de uso reemplazado

    private EndNavigationResult result(String message) { // resultado exitoso simulado
        return new EndNavigationResult(RESERVATION_ID, ReservationStatus.COMPLETED, ARRIVAL,
                VESSEL_ID, OperationalStatus.MAINTENANCE_CLEANING, null, null, message);
    }

    @Test
    void withoutBodyReturnsContractFieldsWithNullDispute() throws Exception { // 200 sin body
        when(endNavigationUseCase.execute(any())).thenReturn(result(MESSAGE_WITH_M1));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation_id").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actual_arrival_at").value("2027-04-10T17:30:00-05:00"))
                .andExpect(jsonPath("$.vessel_id").value(VESSEL_ID.toString()))
                .andExpect(jsonPath("$.vessel_operational_status").value("MAINTENANCE_CLEANING"))
                .andExpect(jsonPath("$.dispute_id").value(nullValue()))
                .andExpect(jsonPath("$.dispute_window_expires_at").value(nullValue()))
                .andExpect(jsonPath("$.message").value(MESSAGE_WITH_M1));
    }

    @Test
    void withBodyMapsArrivalAndRemarks() throws Exception { // 200 con body
        when(endNavigationUseCase.execute(any())).thenReturn(result(MESSAGE_WITH_REMARKS));
        String body = """
                {
                  "actual_arrival_at": "2027-04-10T17:30:00-05:00",
                  "remarks": "Motor revisado"
                }
                """;

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(MESSAGE_WITH_REMARKS));

        ArgumentCaptor<EndNavigationCommand> captor = ArgumentCaptor.forClass(EndNavigationCommand.class);
        verify(endNavigationUseCase).execute(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(captor.getValue().callerId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().actor()).isEqualTo(ActorType.OWNER);
        assertThat(captor.getValue().actualArrivalAt()).isEqualTo(ARRIVAL);
        assertThat(captor.getValue().remarks()).isEqualTo("Motor revisado");
    }

    @Test
    void invalidPathUuidIs400() throws Exception { // uuid invalido en ruta
        mockMvc.perform(post("/api/v1/reservations/not-a-uuid/navigation-end")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));

        verify(endNavigationUseCase, never()).execute(any());
    }

    @Test
    void malformedJsonIs400() throws Exception { // json ilegible
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(endNavigationUseCase, never()).execute(any());
    }

    @Test
    void timestampWithoutOffsetIs400() throws Exception { // offset obligatorio en actual_arrival_at
        String body = """
                {
                  "actual_arrival_at": "2027-04-10T17:30:00"
                }
                """;

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(endNavigationUseCase, never()).execute(any());
    }

    @Test
    void missingIdentityHeadersAre401() throws Exception { // sin headers de identidad
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(endNavigationUseCase, never()).execute(any());
    }

    @Test
    void useCaseErrorsKeepTheirHttpCodes() throws Exception { // 403, 404, 409 y 409
        when(endNavigationUseCase.execute(any()))
                .thenThrow(new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "Not owner"))
                .thenThrow(new ReservationNotFoundException(RESERVATION_ID))
                .thenThrow(new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "Invalid state"))
                .thenThrow(new ReservationRuleException(ErrorCode.TRIP_ALREADY_COMPLETED, "Already completed"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_ALREADY_COMPLETED"));
    }
}