package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
class MaintenanceEventControllerTest { // C5 de extremo a extremo con PostgreSQL real

    private static final Instant NOW = Instant.parse("2026-11-19T15:15:00Z");

    private static final LocalDate RESERVATION_START = LocalDate.of(2026, 12, 20);

    private static final LocalDate RESERVATION_END = LocalDate.of(2026, 12, 22);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateReservationUseCase createReservationUseCase;

    @MockitoSpyBean
    private ChangeReservationStatusUseCase changeReservationStatus;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private FleetInfoPort fleetInfoPort;

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort;

    @MockitoBean
    private ClockPort clockPort;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    void appliesAndCancelsAffectedReservation() throws Exception {
        UUID vesselId = UUID.randomUUID();
        UUID reservationId = createReserved(vesselId, RESERVATION_START, RESERVATION_END);
        String eventId = UUID.randomUUID().toString();

        perform(body(eventId, vesselId, "2026-12-20T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event_id").value(eventId))
                .andExpect(jsonPath("$.outcome").value("APPLIED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, reservationId))
                .isEqualTo(ReservationStatus.CANCELLED.name());
        assertThat(jdbcTemplate.queryForObject("SELECT cancellation_subtype FROM reservation WHERE id = ?",
                String.class, reservationId)).isEqualTo(CancellationSubtype.BY_OWNER.name());
        assertThat(jdbcTemplate.queryForObject("SELECT maintenance_blocked FROM reservation WHERE id = ?",
                Boolean.class, reservationId)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reason FROM reservation_status_audit WHERE reservation_id = ? AND to_status = 'CANCELLED'",
                String.class, reservationId)).isEqualTo("MAINTENANCE");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cancellation_event WHERE reservation_id = ?",
                Integer.class, reservationId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reason FROM cancellation_event WHERE reservation_id = ?", String.class, reservationId))
                .isEqualTo("MAINTENANCE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_message WHERE destination = 'M1_HTTP' AND payload LIKE ?",
                Integer.class, "%" + vesselId + "%")).isZero(); // M1 no recibe AVAILABLE
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_message WHERE destination = 'AMQP' AND payload LIKE ? AND payload LIKE ?",
                Integer.class, "%" + reservationId + "%", "%CANCELLED_BY_HOST%")).isEqualTo(1); // M3 si recibe CANCELLED_BY_HOST

        perform(body(eventId, vesselId, "2026-12-20T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DUPLICATE")); // reenvio idempotente
    }

    @Test
    void noAffectedReservationsKeepsTheReservation() throws Exception {
        UUID vesselId = UUID.randomUUID();
        UUID reservationId = createReserved(vesselId, RESERVATION_START, RESERVATION_END);

        perform(body(UUID.randomUUID().toString(), vesselId, "2026-12-23T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NO_AFFECTED_RESERVATIONS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, reservationId))
                .isEqualTo(ReservationStatus.RESERVED.name());
    }

    @Test
    void pastRangeReturnsInvalidRange() throws Exception {
        UUID vesselId = UUID.randomUUID();
        createReserved(vesselId, RESERVATION_START, RESERVATION_END);

        perform(body(UUID.randomUUID().toString(), vesselId, "2026-11-01T08:00:00-05:00", "2026-11-10T18:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("INVALID_RANGE"));
    }

    @Test
    void unknownVesselReturnsOutcome() throws Exception {
        perform(body(UUID.randomUUID().toString(), UUID.randomUUID(), "2026-12-20T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("UNKNOWN_VESSEL"));
    }

    @Test
    void missingUntilIs400WithErrorBody() throws Exception {
        perform(body(UUID.randomUUID().toString(), UUID.randomUUID(), "2026-12-20T08:00:00-05:00", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_MAINTENANCE_RANGE.getCode()))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void untilBeforeStartIs400() throws Exception {
        perform(body(UUID.randomUUID().toString(), UUID.randomUUID(), "2026-12-27T18:00:00-05:00", "2026-12-20T08:00:00-05:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_MAINTENANCE_RANGE.getCode()));
    }

    @Test
    void invalidVesselIdIs400() throws Exception {
        perform(body(UUID.randomUUID().toString(), "no-es-uuid", "2026-12-20T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETERS.getCode()));
    }

    @Test
    void partialFailureIs503AndRetryCompletes() throws Exception {
        UUID vesselId = UUID.randomUUID();
        UUID first = createReserved(vesselId, RESERVATION_START, RESERVATION_START);
        UUID second = createReserved(vesselId, RESERVATION_START.plusDays(1), RESERVATION_END);
        String eventId = UUID.randomUUID().toString();

        doThrow(new com.seashare.m2.reservas.domain.exception.ReservationRuleException(
                ErrorCode.INTERNAL_ERROR, "boom"))
                .when(changeReservationStatus).execute(argThat(command ->
                        command != null && first.equals(command.reservationId())));

        perform(body(eventId, vesselId, "2026-12-20T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCode.MAINTENANCE_EVENT_PARTIAL_FAILURE.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, second))
                .isEqualTo(ReservationStatus.CANCELLED.name()); // las demas se conservan

        reset(changeReservationStatus); // M1 reintenta con el mismo event_id
        perform(body(eventId, vesselId, "2026-12-20T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("APPLIED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, first))
                .isEqualTo(ReservationStatus.CANCELLED.name());
    }

    private ResultActions perform(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/internal/maintenance-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String body(String eventId, UUID vesselId, String start, String until) {
        return body(eventId, vesselId.toString(), start, until);
    }

    private String body(String eventId, String vesselId, String start, String until) {
        return """
                {
                  "event_id": "%s",
                  "vessel_id": "%s",
                  "maintenance_start": %s,
                  "maintenance_until": %s,
                  "occurred_at": "2026-11-19T15:15:00Z",
                  "reason": "OWNER"
                }
                """.formatted(eventId, vesselId, quote(start), quote(until));
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private UUID createReserved(UUID vesselId, LocalDate start, LocalDate end) {
        UUID renterId = UUID.randomUUID();
        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                renterId,
                UUID.randomUUID().toString(),
                vesselId,
                start,
                end,
                2,
                "Test",
                "3000000000",
                "test@example.com",
                LocalTime.of(9, 0),
                new BigDecimal("1000000.0000")));

        changeReservationStatus.execute(new ChangeReservationStatusCommand(
                view.reservationId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER,
                "Pago iniciado", renterId, null, null, null, null, null));
        changeReservationStatus.execute(new ChangeReservationStatusCommand(
                view.reservationId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM,
                "Pago confirmado"));
        return view.reservationId();
    }
}
