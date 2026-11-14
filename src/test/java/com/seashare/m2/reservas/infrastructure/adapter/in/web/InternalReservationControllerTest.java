package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.application.port.in.GetInternalReservationUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalReservationControllerTest { // pruebas web standalone del endpoint interno sin levantar el contexto

    private static final UUID USER_ID = UUID.randomUUID(); // identidad de servicio para las pruebas

    @Mock
    private GetInternalReservationUseCase useCase; // caso de uso simulado

    private MockMvc mockMvc; // cliente mockmvc standalone

    @BeforeEach
    void setUp() { // registra controlador, resolver de identidad y manejador de errores
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalReservationController(useCase))
                .setCustomArgumentResolvers(new CallerIdentityArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private InternalReservationView sampleView() { // vista completa de una reserva para la respuesta esperada
        return new InternalReservationView(
                UUID.fromString("c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e"), // reserva
                "RS-12345678", // codigo
                UUID.fromString("11111111-1111-1111-1111-111111111111"), // arrendatario
                UUID.fromString("22222222-2222-2222-2222-222222222222"), // embarcacion
                UUID.fromString("33333333-3333-3333-3333-333333333333"), // propietario
                LocalDate.of(2027, 5, 10), // inicio
                LocalDate.of(2027, 5, 15), // fin
                LocalTime.of(9, 0), // zarpe
                "America/Bogota", // zona
                4, // pasajeros
                ReservationStatus.RESERVED, // estado
                null, // subtipo
                null, // ttl
                null, // checkin
                null, // checkout
                null, // novedades
                Instant.parse("2026-10-09T15:00:00Z"), // creacion
                Instant.parse("2026-10-09T15:05:00Z"), // actualizacion
                null, // actor crudo
                6, 5, null, null, false, null); // derivados
    }

    @Test
    void invalidUuidIs400WithInvalidUuidCode() throws Exception { // identificador no uuid en la ruta
        mockMvc.perform(get("/api/v1/internal/reservations/not-a-uuid")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "FINANCE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));
    }

    @Test
    void nonFinanceRoleIs403() throws Exception { // T-13: un perfil distinto de FINANCE no puede consultar
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/internal/reservations/" + id)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
    }

    @Test
    void missingIdentityHeadersIs401() throws Exception { // sin encabezados de identidad
        mockMvc.perform(get("/api/v1/internal/reservations/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void unknownReservationIs404() throws Exception { // reserva inexistente
        UUID id = UUID.randomUUID();
        when(useCase.execute(id)).thenThrow(new ReservationNotFoundException(id));

        mockMvc.perform(get("/api/v1/internal/reservations/" + id)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "FINANCE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void responseHasExactFieldsAndNoMoney() throws Exception { // conjunto exacto de campos snake_case sin montos
        UUID id = UUID.fromString("c1f7a8b2-5e4d-4c3b-8a1e-9f0a2b3c4d5e");
        when(useCase.execute(id)).thenReturn(sampleView());

        MvcResult result = mockMvc.perform(get("/api/v1/internal/reservations/" + id)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "FINANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation_code").value("RS-12345678"))
                .andExpect(jsonPath("$.primary_status").value("RESERVED"))
                .andExpect(jsonPath("$.dates.scheduled_departure").value("2027-05-10T09:00:00-05:00"))
                .andExpect(jsonPath("$.dates.timezone").value("America/Bogota"))
                .andExpect(jsonPath("$.dates.duration_days").value(6))
                .andExpect(jsonPath("$.dates.duration_nights").value(5))
                .andExpect(jsonPath("$.ttl_timer.applies").value(false))
                .andExpect(jsonPath("$.checkin_checkout.damage_detected").isEmpty())
                .andExpect(jsonPath("$..estimated_total").doesNotExist())
                .andExpect(jsonPath("$..rental_amount").doesNotExist())
                .andExpect(jsonPath("$..total_amount").doesNotExist())
                .andExpect(jsonPath("$..insurance_amount").doesNotExist())
                .andExpect(jsonPath("$..guarantee_deposit_amount").doesNotExist())
                .andReturn();

        Map<String, Object> body = new ObjectMapper().readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(body.keySet()).containsExactlyInAnyOrderElementsOf(Set.of(
                "reservation_id", "reservation_code", "renter_id", "vessel_id", "owner_id",
                "dates", "passengers", "primary_status", "cancellation_subtype", "cancellation_actor",
                "cancellation_anticipation_hours", "checkin_checkout", "ttl_timer", "created_at", "updated_at"));

        @SuppressWarnings("unchecked")
        Map<String, Object> dates = (Map<String, Object>) body.get("dates");
        assertThat(dates.keySet()).containsExactlyInAnyOrderElementsOf(Set.of(
                "scheduled_departure", "scheduled_arrival", "timezone", "duration_days", "duration_nights"));

        @SuppressWarnings("unchecked")
        Map<String, Object> checkinCheckout = (Map<String, Object>) body.get("checkin_checkout");
        assertThat(checkinCheckout.keySet()).containsExactlyInAnyOrderElementsOf(Set.of(
                "checkin_at", "checkout_at", "reported_remarks", "damage_detected"));

        @SuppressWarnings("unchecked")
        Map<String, Object> ttlTimer = (Map<String, Object>) body.get("ttl_timer");
        assertThat(ttlTimer.keySet()).containsExactlyInAnyOrderElementsOf(Set.of(
                "applies", "expires_at", "remaining_seconds"));
    }
}
