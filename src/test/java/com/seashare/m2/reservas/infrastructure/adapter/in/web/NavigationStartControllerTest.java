package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.StartNavigationCommand;
import com.seashare.m2.reservas.application.dto.StartNavigationResult;
import com.seashare.m2.reservas.application.port.in.StartNavigationUseCase;
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
class NavigationStartControllerTest { // CU-06: contrato HTTP con caso de uso mockeado

    private static final UUID RESERVATION_ID = UUID.fromString("a1111111-2222-3333-4444-555555555555"); // reserva de prueba

    private static final UUID USER_ID = UUID.fromString("b1111111-2222-3333-4444-555555555555"); // usuario de prueba

    private static final UUID VESSEL_ID = UUID.fromString("c1111111-2222-3333-4444-555555555555"); // embarcacion de prueba

    private static final OffsetDateTime DEPARTURE = OffsetDateTime.parse("2027-04-10T14:05:00-05:00"); // salida con offset

    private static final String MESSAGE = "Inicio de navegacion registrado exitosamente. La reserva ha pasado a En Navegacion y se encolo la notificacion a Modulo 3 para activar el seguro nautico."; // mensaje puntual

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc

    @MockitoBean
    private StartNavigationUseCase startNavigationUseCase; // caso de uso reemplazado

    private StartNavigationResult result() { // resultado exitoso simulado
        return new StartNavigationResult(RESERVATION_ID, ReservationStatus.IN_NAVIGATION, DEPARTURE,
                VESSEL_ID, OperationalStatus.IN_NAVIGATION, true, MESSAGE);
    }

    @Test
    void withoutBodyReturnsContractFields() throws Exception { // 200 sin body
        when(startNavigationUseCase.execute(any())).thenReturn(result());

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation_id").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("IN_NAVIGATION"))
                .andExpect(jsonPath("$.actual_departure_at").value("2027-04-10T14:05:00-05:00"))
                .andExpect(jsonPath("$.vessel_id").value(VESSEL_ID.toString()))
                .andExpect(jsonPath("$.vessel_operational_status").value("IN_NAVIGATION"))
                .andExpect(jsonPath("$.insurance_activated").value(true))
                .andExpect(jsonPath("$.message").value(MESSAGE));
    }

    @Test
    void withBodyMapsDepartureAndDeliveryNotes() throws Exception { // 200 con body
        when(startNavigationUseCase.execute(any())).thenReturn(result());
        String body = """
                {
                  "actual_departure_at": "2027-04-10T14:05:00-05:00",
                  "delivery_notes": "Chalecos revisados"
                }
                """;

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<StartNavigationCommand> captor = ArgumentCaptor.forClass(StartNavigationCommand.class);
        verify(startNavigationUseCase).execute(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(captor.getValue().callerId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().actor()).isEqualTo(ActorType.OWNER);
        assertThat(captor.getValue().actualDepartureAt()).isEqualTo(DEPARTURE);
        assertThat(captor.getValue().deliveryNotes()).isEqualTo("Chalecos revisados");
    }

    @Test
    void invalidPathUuidIs400() throws Exception { // uuid invalido en ruta
        mockMvc.perform(post("/api/v1/reservations/not-a-uuid/navigation-start")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));

        verify(startNavigationUseCase, never()).execute(any());
    }

    @Test
    void malformedJsonIs400() throws Exception { // json ilegible
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(startNavigationUseCase, never()).execute(any());
    }

    @Test
    void timestampWithoutOffsetIs400() throws Exception { // offset obligatorio en actual_departure_at
        String body = """
                {
                  "actual_departure_at": "2027-04-10T14:05:00"
                }
                """;

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(startNavigationUseCase, never()).execute(any());
    }

    @Test
    void missingIdentityHeadersAre401() throws Exception { // sin headers de identidad
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(startNavigationUseCase, never()).execute(any());
    }

    @Test
    void useCaseErrorsKeepTheirHttpCodes() throws Exception { // 403, 404 y cuatro 409
        when(startNavigationUseCase.execute(any()))
                .thenThrow(new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "Not owner"))
                .thenThrow(new ReservationNotFoundException(RESERVATION_ID))
                .thenThrow(new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "Invalid state"))
                .thenThrow(new ReservationRuleException(ErrorCode.TRIP_ALREADY_STARTED, "Already started"))
                .thenThrow(new ReservationRuleException(ErrorCode.EARLY_DEPARTURE, "Early departure"))
                .thenThrow(new ReservationRuleException(ErrorCode.VESSEL_CURRENTLY_IN_NAVIGATION, "Vessel navigating"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_ALREADY_STARTED"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EARLY_DEPARTURE"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VESSEL_CURRENTLY_IN_NAVIGATION"));
    }
}