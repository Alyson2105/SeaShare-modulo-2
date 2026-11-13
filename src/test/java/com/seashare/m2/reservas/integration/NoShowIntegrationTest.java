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
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
class NoShowIntegrationTest { // CU-05: inasistencia real contra PostgreSQL

    private static final Instant SCHEDULED = Instant.parse("2027-04-10T13:30:00Z"); // 08:30 en America/Bogota

    private static final Instant T0 = Instant.parse("2027-04-10T14:00:00Z"); // exactamente 30 minutos despues

    private static final Instant BEFORE_SCHEDULED = SCHEDULED.minusSeconds(1); // un segundo antes del zarpe

    private static final Instant BEFORE_GRACE_END = T0.minusSeconds(1); // un segundo antes de los 30 minutos

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

    private Seed reserved() { // siembra hasta RESERVED con la embarcacion por defecto
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation(renterId, vesselId, START_DATE, END_DATE, DEPARTURE_TIME);
        toPendingPayment(reservationId, renterId);
        toReserved(reservationId, renterId);
        return new Seed(reservationId, renterId, vesselId);
    }

    private Seed pendingPayment() { // siembra hasta PENDING_PAYMENT
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation(renterId, vesselId, START_DATE, END_DATE, DEPARTURE_TIME);
        toPendingPayment(reservationId, renterId);
        return new Seed(reservationId, renterId, vesselId);
    }

    private String bodyWithObservations(String observations) { // body con observaciones
        return """
                {
                  "observations": "%s"
                }
                """.formatted(observations);
    }

    private ResultActions performNoShow(UUID reservationId, UUID userId, String role, String observations) throws Exception { // llama al endpoint
        MockHttpServletRequestBuilder request = post("/api/v1/reservations/{reservation_id}/no-show", reservationId)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role);
        if (observations != null) { // body opcional
            request.contentType(MediaType.APPLICATION_JSON); // json explicito
            request.content(bodyWithObservations(observations)); // cuerpo con observaciones
        }
        return mockMvc.perform(request);
    }

    private Integer noShowCount(UUID reservationId) { // cuenta filas de inasistencia
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM noshow_event WHERE reservation_id = ?", Integer.class, reservationId);
    }

    @Test
    void successfulNoShowPersistsEventAndCancelsReservation() throws Exception { // caso 1
        Seed seed = reserved();

        performNoShow(seed.reservationId(), ownerId, "OWNER", "No se presento en el muelle")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellation_subtype").value("NO_SHOW"))
                .andExpect(jsonPath("$.wait_minutes_recorded").value(30))
                .andExpect(jsonPath("$.vessel_operational_status").value("AVAILABLE"));

        Reservation saved = reservationRepository.findById(seed.reservationId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(saved.getCancellationSubtype()).isEqualTo(CancellationSubtype.NO_SHOW);

        assertThat(noShowCount(seed.reservationId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT owner_id FROM noshow_event WHERE reservation_id = ?", UUID.class, seed.reservationId()))
                .isEqualTo(ownerId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT waiting_minutes FROM noshow_event WHERE reservation_id = ?", Integer.class, seed.reservationId()))
                .isEqualTo(30);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT observations FROM noshow_event WHERE reservation_id = ?", String.class, seed.reservationId()))
                .isEqualTo("No se presento en el muelle");
    }

    @Test
    void secondClickIsInvalidStateAndKeepsOneRow() throws Exception { // caso 2
        Seed seed = reserved();

        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isOk());
        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));

        assertThat(noShowCount(seed.reservationId())).isEqualTo(1);
    }

    @Test
    void oneSecondBeforeGraceEndIsRejected() throws Exception { // caso 3
        Seed seed = reserved();
        when(clockPort.now()).thenReturn(BEFORE_GRACE_END);

        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRACE_PERIOD_NOT_ELAPSED"));

        assertThat(reservationRepository.findById(seed.reservationId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RESERVED);
        assertThat(noShowCount(seed.reservationId())).isZero();
    }

    @Test
    void beforeScheduledDepartureIsTripNotStarted() throws Exception { // caso 4
        Seed seed = reserved();
        when(clockPort.now()).thenReturn(BEFORE_SCHEDULED);

        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_STARTED"));

        assertThat(noShowCount(seed.reservationId())).isZero();
    }

    @Test
    void renterAndForeignOwnerAreForbiddenAndUnknownIs404() throws Exception { // caso 5
        Seed seed = reserved();

        performNoShow(seed.reservationId(), seed.renterId(), "RENTER", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
        performNoShow(seed.reservationId(), UUID.randomUUID(), "OWNER", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
        performNoShow(UUID.randomUUID(), ownerId, "OWNER", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void pendingPaymentReservationIsInvalidState() throws Exception { // caso 6
        Seed seed = pendingPayment();

        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));

        assertThat(noShowCount(seed.reservationId())).isZero();
    }

    @Test
    void invalidHarborTimezoneReturns503() throws Exception { // caso 7
        Seed seed = reserved();
        jdbcTemplate.update("UPDATE reservation SET harbor_timezone = ? WHERE id = ?", "Mars/Phobos", seed.reservationId());
        entityManager.clear(); // evita leer la entidad cacheada antes de la actualizacion directa

        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("HARBOR_TIMEZONE_UNAVAILABLE"));

        assertThat(noShowCount(seed.reservationId())).isZero();
    }

    @Test
    void observationsOver500AreRejected() throws Exception { // caso 8
        Seed seed = reserved();

        performNoShow(seed.reservationId(), ownerId, "OWNER", "a".repeat(501))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        assertThat(noShowCount(seed.reservationId())).isZero();
    }

    @Test
    void navigationStartAfterNoShowIsInvalidState() throws Exception { // caso 9
        Seed seed = reserved();
        performNoShow(seed.reservationId(), ownerId, "OWNER", null)
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-start", seed.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_STATE"));
    }

    private record Seed(UUID reservationId, UUID renterId, UUID vesselId) { // datos de la reserva sembrada
    }
}