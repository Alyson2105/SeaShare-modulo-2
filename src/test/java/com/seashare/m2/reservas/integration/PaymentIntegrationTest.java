package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
@Transactional
class PaymentIntegrationTest { // CU-03: inicio de pago de punta a punta con adaptadores falsos

    private static final String VALID_BODY = """
            {
              "accepts_cancellation_policy": true,
              "payment": {
                "payment_token_ref": "tok_12345abcdef",
                "payment_method_type": "CREDIT_CARD",
                "payment_metadata": {
                  "payer_email": "carlos@example.com",
                  "payment_method_id": "visa",
                  "installments": 1,
                  "last_four": "4242"
                }
              }
            }
            """; // cuerpo valido del contrato

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateReservationUseCase createReservationUseCase;

    @MockitoBean
    private FleetInfoPort fleetInfoPort;

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort;

    @MockitoSpyBean
    private OutboxPort outboxPort;

    private UUID ownerId;

    private UUID renterId;

    private UUID vesselId;

    @BeforeEach
    void setUp() { // flota y disponibilidad simuladas
        ownerId = UUID.randomUUID();
        renterId = UUID.randomUUID();
        vesselId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private ReservationView createInitiated(LocalDate start, LocalDate end) { // siembra una reserva INITIATED
        return createReservationUseCase.execute(new CreateReservationCommand(
                renterId,
                UUID.randomUUID().toString(),
                vesselId,
                start,
                end,
                2,
                "Juan Perez",
                "+573001234567",
                "juan@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("2500000.00")));
    }

    private org.springframework.test.web.servlet.ResultActions pay(UUID reservationId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-User-Id", renterId.toString())
                .header("X-User-Role", "RENTER"));
    }

    @Test
    void startPaymentTransitionsAndReturnsBreakdown() throws Exception { // camino feliz del contrato
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));
        clearInvocations(outboxPort);

        pay(reservation.reservationId(), VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.reservation_id").value(reservation.reservationId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.vessel_id").value(vesselId.toString()))
                .andExpect(jsonPath("$.total_calculation.currency").value("COP"))
                .andExpect(jsonPath("$.total_calculation.total_amount").value(10040000.00))
                .andExpect(jsonPath("$.total_calculation.breakdown.rental_amount").value(9600000.00))
                .andExpect(jsonPath("$.total_calculation.breakdown.insurance_amount").value(120000.00))
                .andExpect(jsonPath("$.total_calculation.breakdown.guarantee_deposit_amount").value(320000.00))
                .andExpect(jsonPath("$.payment.charge_status").value("IN_PROCESS"))
                .andExpect(jsonPath("$.payment.attempt").value(1))
                .andExpect(jsonPath("$.payment.confirmation_mode").value("QUERY"))
                .andExpect(jsonPath("$.ttl_remaining_seconds").isNumber())
                .andExpect(jsonPath("$.ttl_expires_at").isNotEmpty());
    }

    @Test
    void pendingEventCarriesPaymentToken() throws Exception { // FR-009 y CU-14
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));
        clearInvocations(outboxPort);

        pay(reservation.reservationId(), VALID_BODY).andExpect(status().isOk());

        verify(outboxPort).enqueue(argThat((StatusChangedEvent event) ->
                event.reservationId().equals(reservation.reservationId())
                        && event.status() == ReservationStatus.PENDING_PAYMENT
                        && "tok_12345abcdef".equals(event.paymentTokenRef()))); // token opaco hacia M3
    }

    @Test
    void breakdownIsPersistedForDetailView() throws Exception { // CU-21 deja de mostrar rubros nulos
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));

        pay(reservation.reservationId(), VALID_BODY).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reservations/" + reservation.reservationId())
                        .header("X-User-Id", renterId.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment_summary.rental_amount").value(9600000.00))
                .andExpect(jsonPath("$.payment_summary.insurance_amount").value(120000.00))
                .andExpect(jsonPath("$.payment_summary.guarantee_deposit_amount").value(320000.00))
                .andExpect(jsonPath("$.payment_summary.total_paid").value(10040000.00));
    }

    @Test
    void policyNotAcceptedIs400() throws Exception { // FR-014
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));
        String body = VALID_BODY.replace("\"accepts_cancellation_policy\": true",
                "\"accepts_cancellation_policy\": false");

        pay(reservation.reservationId(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POLICY_NOT_ACCEPTED"));
    }

    @Test
    void missingPaymentTokenIs400() throws Exception { // datos incompletos
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));
        String body = VALID_BODY.replace("\"payment_token_ref\": \"tok_12345abcdef\"",
                "\"payment_token_ref\": \"\"");

        pay(reservation.reservationId(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_DATA_INCOMPLETE"));
    }

    @Test
    void secondPaymentIsConflict() throws Exception { // FR-002: ya no esta INITIATED
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));
        pay(reservation.reservationId(), VALID_BODY).andExpect(status().isOk());

        pay(reservation.reservationId(), VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
    }

    @Test
    void overlappingReservationLosesTheFcfsRace() throws Exception { // FR-008 y SC-002
        ReservationView winner = createInitiated(LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 3));
        ReservationView loser = createInitiated(LocalDate.of(2027, 7, 2), LocalDate.of(2027, 7, 4));

        pay(winner.reservationId(), VALID_BODY).andExpect(status().isOk());

        pay(loser.reservationId(), VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENCY_CONFLICT"));
    }

    @Test
    void ownerCannotPay() throws Exception { // solo el arrendatario titular
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));

        mockMvc.perform(post("/api/v1/reservations/" + reservation.reservationId() + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY)
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_OWNED"));
    }

    @Test
    void ttlIsNotRestarted() throws Exception { // FR-007 y SC-001
        ReservationView reservation = createInitiated(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));

        String body = pay(reservation.reservationId(), VALID_BODY)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.time.OffsetDateTime returnedTtl = java.time.OffsetDateTime.parse(
                tools.jackson.databind.json.JsonMapper.builder().build()
                        .readTree(body).path("ttl_expires_at").stringValue());

        assertThat(returnedTtl.toInstant()).isEqualTo(reservation.ttlExpiresAt()); // mismo vencimiento original
        assertThat(reservation.ttlExpiresAt())
                .isBeforeOrEqualTo(java.time.Instant.now().plusSeconds(900)); // nace en CU-02
    }
}
