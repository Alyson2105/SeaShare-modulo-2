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
import java.sql.Timestamp;
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
class NavigationStartIntegrationTest { // CU-06: inicio real contra PostgreSQL

    private static final Instant T0 = Instant.parse("2027-04-10T14:00:00Z"); // 09:00 en America/Bogota

    private static final Instant BEFORE_SCHEDULED = Instant.parse("2027-04-10T13:00:00Z"); // 08:00 en America/Bogota

    private static final OffsetDateTime DEPARTURE = OffsetDateTime.parse("2027-04-10T08:35:00-05:00"); // salida real

    private static final LocalDate START_DATE = LocalDate.of(2027, 4, 10); // fecha de inicio

    private static final LocalDate END_DATE = LocalDate.of(2027, 4, 12); // fecha de fin

    private static final LocalTime DEPARTURE_TIME = LocalTime.of(8, 30); // hora pactada

    private static final String ZONE = "America/Bogota"; // zona del puerto

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

    private UUID vesselId; // embarcacion de la siembra por defecto

    @BeforeEach
    void setUp() { // prepara mocks de M1 y reloj
        ownerId = UUID.randomUUID();
        vesselId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(T0);
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, ZONE));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private UUID createReservation(UUID renterId, UUID targetVesselId, LocalDate startDate, LocalDate endDate, LocalTime departureTime) { // siembra una reserva INITIATED
        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                renterId,
                UUID.randomUUID().toString(),
                targetVesselId,
                startDate,
                endDate,
                2,
                "Juan Perez",
                "+573001234567",
                "juan@example.com",
                departureTime,
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

    private void toInNavigation(UUID reservationId) { // RESERVED -> IN_NAVIGATION
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.IN_NAVIGATION, null, ActorType.OWNER, "Inicio de navegacion",
                ownerId, null, null, null, null, null));
    }

    private Seed reserved() { // siembra hasta RESERVED con la embarcacion por defecto
        return reserved(vesselId, START_DATE, END_DATE, DEPARTURE_TIME);
    }

    private Seed reserved(UUID targetVesselId, LocalDate startDate, LocalDate endDate, LocalTime departureTime) { // siembra hasta RESERVED
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation(renterId, targetVesselId, startDate, endDate, departureTime);
        toPendingPayment(reservationId, renterId);
        toReserved(reservationId, renterId);
        return new Seed(reservationId, renterId, targetVesselId);
    }

    private Seed pendingPayment() { // siembra hasta PENDING_PAYMENT
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation(renterId, vesselId, START_DATE, END_DATE, DEPARTURE_TIME);
        toPendingPayment(reservationId, renterId);
        return new Seed(reservationId, renterId, vesselId);
    }

    private String bodyWithNotes(String notes) { // body con salida y notas
        return """
                {
                  "actual_departure_at": "2027-04-10T08:35:00-05:00",
                  "delivery_notes": "%s"
                }
                """.formatted(notes);
    }

    private String bodyWithoutNotes() { // body solo con salida
        return """
                {
                  "actual_departure_at": "2027-04-10T08:35:00-05:00"
                }
                """;
    }

    private String bodyDepartingAt(String departureAt) { // body con la salida indicada
        return """
                {
                  "actual_departure_at": "%s"
                }
                """.formatted(departureAt);
    }

    @Test
    void successfulStartPersistsDepartureStateAndCheckinEvent() throws Exception { // caso 1
        Seed seed = reserved();

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithNotes("Chalecos revisados")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_NAVIGATION"))
                .andExpect(jsonPath("$.actual_departure_at").value("2027-04-10T08:35:00-05:00"))
                .andExpect(jsonPath("$.vessel_operational_status").value("IN_NAVIGATION"))
                .andExpect(jsonPath("$.insurance_activated").value(true));

        Reservation saved = reservationRepository.findById(seed.reservationId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.IN_NAVIGATION);
        assertThat(saved.getActualDepartureAt()).isEqualTo(DEPARTURE.toInstant());

        Integer checkinRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM checkin_event WHERE reservation_id = ?", Integer.class, seed.reservationId());
        assertThat(checkinRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_id FROM checkin_event WHERE reservation_id = ?", UUID.class, seed.reservationId()))
                .isEqualTo(ownerId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT notes FROM checkin_event WHERE reservation_id = ?", String.class, seed.reservationId()))
                .isEqualTo("Chalecos revisados");
        Timestamp actualTime = jdbcTemplate.queryForObject(
                "SELECT actual_time FROM checkin_event WHERE reservation_id = ?", Timestamp.class, seed.reservationId());
        assertThat(actualTime.toInstant()).isEqualTo(DEPARTURE.toInstant());
    }

    @Test
    void secondClickReturnsTripAlreadyStarted() throws Exception { // caso 2
        Seed seed = reserved();

        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isOk());
        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_ALREADY_STARTED"));
    }

    @Test
    void clockBeforeScheduledDepartureReturnsEarlyDeparture() throws Exception { // caso 3
        Seed seed = reserved();
        when(clockPort.now()).thenReturn(BEFORE_SCHEDULED);

        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EARLY_DEPARTURE"));

        Reservation saved = reservationRepository.findById(seed.reservationId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(checkinCount(seed.reservationId())).isZero();
    }

    @Test
    void notesOver1000AreRejected() throws Exception { // caso 4
        Seed seed = reserved();

        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithNotes("a".repeat(1001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        assertThat(checkinCount(seed.reservationId())).isZero();
    }

    @Test
    void renterAndForeignOwnerAreForbiddenAndUnknownIs404() throws Exception { // caso 5
        Seed seed = reserved();

        performStart(seed.reservationId(), seed.renterId(), "RENTER", bodyWithoutNotes())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
        performStart(seed.reservationId(), UUID.randomUUID(), "OWNER", bodyWithoutNotes())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
        performStart(UUID.randomUUID(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void pendingPaymentReservationIsInvalidState() throws Exception { // caso 6
        Seed seed = pendingPayment();

        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));

        assertThat(checkinCount(seed.reservationId())).isZero();
    }

    @Test
    void invalidHarborTimezoneReturns503() throws Exception { // caso 7
        Seed seed = reserved();
        jdbcTemplate.update("UPDATE reservation SET harbor_timezone = ? WHERE id = ?", "Mars/Phobos", seed.reservationId());
        entityManager.clear(); // evita leer la entidad cacheada antes de la actualizacion directa

        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("HARBOR_TIMEZONE_UNAVAILABLE"));

        assertThat(checkinCount(seed.reservationId())).isZero();
    }

    @Test
    void anotherReservationOfSameVesselInNavigationIsRejected() throws Exception { // caso 8
        Seed first = reserved(vesselId, START_DATE, END_DATE, DEPARTURE_TIME);
        toInNavigation(first.reservationId());

        Instant secondTime = Instant.parse("2027-05-20T14:00:00Z"); // despues del zarpe de la segunda reserva
        when(clockPort.now()).thenReturn(secondTime);
        Seed second = reserved(vesselId, LocalDate.of(2027, 5, 20), LocalDate.of(2027, 5, 22), DEPARTURE_TIME);

        performStart(second.reservationId(), ownerId, "OWNER", bodyDepartingAt("2027-05-20T08:35:00-05:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VESSEL_CURRENTLY_IN_NAVIGATION"));

        assertThat(reservationRepository.findById(first.reservationId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.IN_NAVIGATION);
        assertThat(reservationRepository.findById(second.reservationId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RESERVED);
        assertThat(checkinCount(second.reservationId())).isZero();
    }

    @Test
    void renterCancellationAfterStartIsRejected() throws Exception { // caso 9
        Seed seed = reserved();
        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/cancellation", seed.reservationId())
                        .header("X-User-Id", seed.renterId().toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
    }

    @Test
    void navigationEndAfterStartSucceeds() throws Exception { // caso 10
        Seed seed = reserved();
        performStart(seed.reservationId(), ownerId, "OWNER", bodyWithoutNotes())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private org.springframework.test.web.servlet.ResultActions performStart(UUID reservationId,
                                                                           UUID userId,
                                                                           String role,
                                                                           String body) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", reservationId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Integer checkinCount(UUID reservationId) { // cuenta filas de check-in de la reserva
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM checkin_event WHERE reservation_id = ?", Integer.class, reservationId);
    }

    private record Seed(UUID reservationId, UUID renterId, UUID vesselId) { // datos de la reserva sembrada
    }
}