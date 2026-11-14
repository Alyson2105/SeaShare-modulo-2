package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.domain.ClockPort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
@Transactional
class NavigationEndIntegrationTest { // CU-07: cierre real contra PostgreSQL

    private static final Instant T0 = Instant.parse("2026-11-12T10:00:00Z"); // reloj controlado

    private static final OffsetDateTime ARRIVAL = OffsetDateTime.parse("2027-04-10T17:30:00-05:00"); // llegada con offset

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // siembra reservas

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // siembra estados

    @Autowired
    private ReservationRepositoryPort reservationRepository; // lectura de la reserva persistida

    @Autowired
    private JdbcTemplate jdbcTemplate; // verificacion de tablas fisicas

    @Autowired
    private EntityManager entityManager; // limpia la cache tras una actualizacion directa

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad

    @MockitoBean
    private ClockPort clockPort; // reloj controlado

    private UUID ownerId; // propietario devuelto por M1

    @BeforeEach
    void setUp() { // prepara mocks de M1 y reloj
        ownerId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(T0);
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private UUID createReservation() { // siembra una reserva INITIATED
        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12),
                2,
                "Juan Perez",
                "+573001234567",
                "juan@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("5000000.0000")
        ));
        return view.reservationId();
    }

    private void toPendingPayment(UUID reservationId, UUID renterId) { // INITIATED -> PENDING_PAYMENT
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                renterId, null, null, null, null, null));
    }

    private void toReserved(UUID reservationId, UUID renterId) { // PENDING_PAYMENT -> RESERVED
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.RESERVED, null, ActorType.RENTER, "Pago confirmado",
                renterId, null, null, null, null, null));
    }

    private void toInNavigation(UUID reservationId, UUID ownerId) { // RESERVED -> IN_NAVIGATION
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.IN_NAVIGATION, null, ActorType.OWNER, "Inicio de navegacion",
                ownerId, null, null, null, null, null));
    }

    private Seed inNavigation() { // siembra completa hasta IN_NAVIGATION
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation();
        toPendingPayment(reservationId, renterId);
        toReserved(reservationId, renterId);
        toInNavigation(reservationId, ownerId);
        return new Seed(reservationId, renterId);
    }

    private Seed reserved() { // siembra hasta RESERVED
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation();
        toPendingPayment(reservationId, renterId);
        toReserved(reservationId, renterId);
        return new Seed(reservationId, renterId);
    }

    private String bodyWithRemarks(String remarks) { // body con llegada y novedades
        return """
                {
                  "actual_arrival_at": "2027-04-10T17:30:00-05:00",
                  "remarks": "%s"
                }
                """.formatted(remarks);
    }

    @Test
    void successfulEndPersistsArrivalCompletedStateAndCheckoutEvent() throws Exception { // caso 1
        Seed seed = inNavigation();
        String body = bodyWithRemarks("Llegada sin novedad");

        String response = mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actual_arrival_at").value("2027-04-10T17:30:00-05:00"))
                .andExpect(jsonPath("$.dispute_id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        var dispute = jdbcTemplate.queryForMap(
                "SELECT id, status, window_start, window_end FROM dispute_guarantee WHERE reservation_id = ?",
                seed.reservationId());
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(response, "$.dispute_id"))
                .isEqualTo(dispute.get("id").toString());
        assertThat(dispute.get("status")).isEqualTo("PENDING");
        assertThat(((java.sql.Timestamp) dispute.get("window_start")).toInstant()).isEqualTo(T0);
        assertThat(((java.sql.Timestamp) dispute.get("window_end")).toInstant()).isEqualTo(T0.plusSeconds(86400));
        assertThat(OffsetDateTime.parse(com.jayway.jsonpath.JsonPath.read(response, "$.dispute_window_expires_at"))
                .toInstant()).isEqualTo(T0.plusSeconds(86400));

        Reservation saved = reservationRepository.findById(seed.reservationId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(saved.getActualArrivalAt()).isEqualTo(Instant.parse("2027-04-10T22:30:00Z"));

        Integer checkoutRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM checkout_event WHERE reservation_id = ?", Integer.class, seed.reservationId());
        assertThat(checkoutRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_id FROM checkout_event WHERE reservation_id = ?", UUID.class, seed.reservationId()))
                .isEqualTo(ownerId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT remarks FROM checkout_event WHERE reservation_id = ?", String.class, seed.reservationId()))
                .isEqualTo("Llegada sin novedad");
        OffsetDateTime actualTime = jdbcTemplate.queryForObject(
                "SELECT actual_time FROM checkout_event WHERE reservation_id = ?",
                (rs, rowNum) -> rs.getObject(1, OffsetDateTime.class), seed.reservationId());
        assertThat(actualTime.toInstant()).isEqualTo(Instant.parse("2027-04-10T22:30:00Z"));
    }

    @Test
    void secondClickReturnsTripAlreadyCompleted() throws Exception { // caso 2
        Seed seed = inNavigation();
        String body = bodyWithRemarks("Cierre");

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        entityManager.flush();
        Integer outboxBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_message WHERE payload LIKE ?", Integer.class,
                "%" + seed.reservationId() + "%");

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_ALREADY_COMPLETED"));

        entityManager.flush();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM checkout_event WHERE reservation_id = ?",
                Integer.class, seed.reservationId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM dispute_guarantee WHERE reservation_id = ?",
                Integer.class, seed.reservationId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_message WHERE payload LIKE ?",
                Integer.class, "%" + seed.reservationId() + "%")).isEqualTo(outboxBefore);
    }

    @Test
    void remarksOver2000AreRejected() throws Exception { // caso 3
        Seed seed = inNavigation();

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithRemarks("a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void renterRoleAndForeignOwnerAreForbidden() throws Exception { // caso 4
        Seed seed = inNavigation();
        String body = bodyWithRemarks("Cierre");

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", seed.renterId().toString())
                        .header("X-User-Role", "RENTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
    }

    @Test
    void unknownReservationIs404() throws Exception { // caso 5
        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", UUID.randomUUID())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithRemarks("Cierre")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void reservedReservationIsInvalidState() throws Exception { // caso 6
        Seed seed = reserved();

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithRemarks("Cierre")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
    }

    @Test
    void invalidHarborTimezoneReturns503() throws Exception { // caso 7
        Seed seed = inNavigation();
        jdbcTemplate.update("UPDATE reservation SET harbor_timezone = ? WHERE id = ?", "Mars/Phobos", seed.reservationId());
        entityManager.clear(); // evita leer la entidad cacheada antes de la actualizacion directa

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithRemarks("Cierre")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("HARBOR_TIMEZONE_UNAVAILABLE"));

        Integer checkoutRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM checkout_event WHERE reservation_id = ?", Integer.class, seed.reservationId());
        assertThat(checkoutRows).isZero();
    }

    private record Seed(UUID reservationId, UUID renterId) { // datos de la reserva sembrada
    }
}
