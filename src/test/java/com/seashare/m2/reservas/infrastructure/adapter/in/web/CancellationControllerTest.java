package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.CancellationResult;
import com.seashare.m2.reservas.application.dto.RequestCancellationCommand;
import com.seashare.m2.reservas.application.port.in.RequestCancellationUseCase;
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
class CancellationControllerTest { // pruebas MockMvc de CU-04

    private static final UUID RESERVATION_ID = UUID.fromString("a1111111-2222-3333-4444-555555555555"); // reserva de prueba

    private static final UUID USER_ID = UUID.fromString("b1111111-2222-3333-4444-555555555555"); // usuario de prueba

    private static final UUID VESSEL_ID = UUID.fromString("c1111111-2222-3333-4444-555555555555"); // embarcacion de prueba

    private static final OffsetDateTime DEPARTURE = OffsetDateTime.parse("2027-04-10T14:00:00-05:00"); // zarpe con offset

    private static final OffsetDateTime CANCELLED_AT = OffsetDateTime.parse("2027-04-07T14:00:00-05:00"); // solicitud con offset

    private static final String FLEXIBLE_MESSAGE = "Reserva cancelada con 72 horas o más de anticipación. Clasificación Flexible aplicada. El activo náutico ha quedado Disponible."; // mensaje flexible

    private static final String LATE_MESSAGE = "Reserva cancelada con menos de 24 horas de anticipación. Clasificación Tardío aplicada. Módulo 3 tramitará la liquidación sin reembolso al arrendatario."; // mensaje tardio

    private static final String OWNER_MAINTENANCE_MESSAGE = "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. La embarcación ha sido transferida a 'En Mantenimiento/Limpieza' en Módulo 1 para inhabilitar nuevas reservas. Las reservas futuras confirmadas se mantienen intactas (sin cancelación en cascada)."; // mensaje de mantenimiento

    private static final String OWNER_MECHANICAL_MESSAGE = "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. Avería mecánica registrada."; // mensaje de averia

    private static final String OWNER_LOGISTICS_MESSAGE = "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. La embarcación se mantiene Disponible al no mediar avería mecánica."; // mensaje de fuerza mayor

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc

    @MockitoBean
    private RequestCancellationUseCase requestCancellationUseCase; // caso de uso reemplazado

    private CancellationResult renterResult(CancellationSubtype subtype, double anticipationHours) { // resultado del arrendatario
        return new CancellationResult(RESERVATION_ID, ReservationStatus.CANCELLED, subtype, ActorType.RENTER,
                anticipationHours, "America/Bogota", DEPARTURE, CANCELLED_AT, null, "Renter justification",
                VESSEL_ID, OperationalStatus.AVAILABLE, "ENQUEUED", "PUBLISHED");
    }

    private CancellationResult ownerResult(String reason, OperationalStatus newStatus) { // resultado del propietario
        String m1 = newStatus == null ? null : "ENQUEUED"; // sin aviso a M1 cuando no hay estado
        return new CancellationResult(RESERVATION_ID, ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER,
                ActorType.OWNER, null, "America/Bogota", DEPARTURE, CANCELLED_AT, reason, "Motor averiado",
                VESSEL_ID, newStatus, m1, "PUBLISHED");
    }

    @Test
    void renterWithoutBodyReturnsAllContractFields() throws Exception { // 200 y 15 claves snake_case
        when(requestCancellationUseCase.execute(any())).thenReturn(renterResult(CancellationSubtype.FLEXIBLE, 72.0));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation_id").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellation_subtype").value("FLEXIBLE"))
                .andExpect(jsonPath("$.requesting_actor").value("RENTER"))
                .andExpect(jsonPath("$.anticipation_hours").value(72.0))
                .andExpect(jsonPath("$.port_timezone").value("America/Bogota"))
                .andExpect(jsonPath("$.scheduled_departure").value("2027-04-10T14:00:00-05:00"))
                .andExpect(jsonPath("$.cancelled_at").value("2027-04-07T14:00:00-05:00"))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.justification").value("Renter justification"))
                .andExpect(jsonPath("$.vessel_id").value(VESSEL_ID.toString()))
                .andExpect(jsonPath("$.vessel_new_operational_status").value("AVAILABLE"))
                .andExpect(jsonPath("$.m1_sync_status").value("ENQUEUED"))
                .andExpect(jsonPath("$.m3_notification_status").value("PUBLISHED"))
                .andExpect(jsonPath("$.message").value(FLEXIBLE_MESSAGE));
    }

    @Test
    void ownerBodyIsTranslatedToCommand() throws Exception { // cuerpo del propietario
        when(requestCancellationUseCase.execute(any()))
                .thenReturn(ownerResult("mechanical_failure", OperationalStatus.MAINTENANCE_CLEANING));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"mechanical_failure\",\"justification\":\"Motor averiado\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<RequestCancellationCommand> captor = ArgumentCaptor.forClass(RequestCancellationCommand.class);
        verify(requestCancellationUseCase).execute(captor.capture());
        assertCommand(captor.getValue(), ActorType.OWNER, "mechanical_failure", "Motor averiado");
    }

    @Test
    void invalidPathUuidIs400() throws Exception { // uuid invalido en ruta
        mockMvc.perform(post("/api/v1/reservations/not-a-uuid/cancellation")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));

        verify(requestCancellationUseCase, never()).execute(any());
    }

    @Test
    void tooLongJustificationIs400() throws Exception { // limite de jakarta validation
        String body = "{\"justification\":\"" + "a".repeat(501) + "\"}"; // 501 caracteres

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(requestCancellationUseCase, never()).execute(any());
    }

    @Test
    void oversizedReasonIs400() throws Exception { // limite de texto libre
        String body = "{\"reason\":\"" + "x".repeat(501) + "\"}";

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        verify(requestCancellationUseCase, never()).execute(any());
    }

    @Test
    void financeRoleIs403() throws Exception { // rol no autorizado
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "FINANCE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));

        verify(requestCancellationUseCase, never()).execute(any());
    }

    @Test
    void notOwnedErrorIs403() throws Exception { // error de dominio de pertenencia
        when(requestCancellationUseCase.execute(any()))
                .thenThrow(new ReservationRuleException(ErrorCode.RESERVATION_NOT_OWNED, "Not owned"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_OWNED"));
    }

    @Test
    void invalidStateErrorIs409() throws Exception { // error de estado invalido
        when(requestCancellationUseCase.execute(any()))
                .thenThrow(new ReservationRuleException(ErrorCode.INVALID_RESERVATION_STATE, "Invalid state"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
    }

    @Test
    void messagesCoverOwnerCasesAndLate() throws Exception { // textos de 4.9
        CancellationResult maintenance = ownerResult("mechanical_failure", OperationalStatus.MAINTENANCE_CLEANING);
        CancellationResult mechanicalNoM1 = ownerResult("mechanical_failure", null);
        CancellationResult forceMajeure = ownerResult("logistics_force_majeure", OperationalStatus.AVAILABLE);
        CancellationResult late = renterResult(CancellationSubtype.LATE, 8.5);
        when(requestCancellationUseCase.execute(any()))
                .thenReturn(maintenance, mechanicalNoM1, forceMajeure, late);

        performOwnerCancellation("mechanical_failure")
                .andExpect(jsonPath("$.message").value(OWNER_MAINTENANCE_MESSAGE)); // averia con mantenimiento
        performOwnerCancellation("mechanical_failure")
                .andExpect(jsonPath("$.message").value(OWNER_MECHANICAL_MESSAGE)); // averia sin aviso a M1
        performOwnerCancellation("logistics_force_majeure")
                .andExpect(jsonPath("$.message").value(OWNER_LOGISTICS_MESSAGE)); // fuerza mayor
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(LATE_MESSAGE)); // politica tardia
    }

    private org.springframework.test.web.servlet.ResultActions performOwnerCancellation(String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", RESERVATION_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    private void assertCommand(RequestCancellationCommand command, ActorType actor, String reason, String justification) {
        org.assertj.core.api.Assertions.assertThat(command.reservationId()).isEqualTo(RESERVATION_ID);
        org.assertj.core.api.Assertions.assertThat(command.callerId()).isEqualTo(USER_ID);
        org.assertj.core.api.Assertions.assertThat(command.actor()).isEqualTo(actor);
        org.assertj.core.api.Assertions.assertThat(command.reason()).isEqualTo(reason);
        org.assertj.core.api.Assertions.assertThat(command.justification()).isEqualTo(justification);
    }
}