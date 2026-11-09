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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
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
class CreateReservationTest { // pruebas funcionales del caso de uso iniciar reserva CU-02 usando MockMvc y puertos

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para realizar peticiones rest

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso para sembrar reservas previas

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // caso de uso para llevar reservas a estados bloqueantes

    @Autowired
    private ReservationRepositoryPort reservationRepositoryPort; // puerto de lectura para verificar persistencia

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de informacion de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion del puerto de estado operativo

    private final ObjectMapper objectMapper = new ObjectMapper(); // serializador json para analizar respuestas
    private UUID defaultOwnerId; // identificador del propietario para mocks por defecto

    @BeforeEach
    void setUp() { // inicializa mocks de modulo 1 antes de cada prueba
        defaultOwnerId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(defaultOwnerId, 12, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private String buildRequestBody(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers,
                                    String departureTime, String estimatedTotal, String name, String phone, String email) { // genera json snake_case
        String emailField = email != null ? ",\"email\":\"" + email + "\"" : "";
        return """
                {
                  "vessel_id": "%s",
                  "start_date": "%s",
                  "end_date": "%s",
                  "passengers": %d,
                  "departure_time": "%s",
                  "estimated_total": "%s",
                  "holder": {
                    "full_name": "%s",
                    "phone": "%s"%s
                  }
                }
                """.formatted(vesselId, startDate, endDate, passengers, departureTime, estimatedTotal, name, phone, emailField);
    }

    private ReservationView createReservation(UUID renterId, UUID vesselId, LocalDate startDate, LocalDate endDate) { // siembra una reserva INITIATED
        return createReservationUseCase.execute(new CreateReservationCommand(
                renterId,
                UUID.randomUUID().toString(),
                vesselId,
                startDate,
                endDate,
                2,
                "Reserva Previa",
                "+573009998877",
                "previa@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("3000000.0000")
        ));
    }

    @Test
    void createsReservationInInitiated() throws Exception { // crea reserva formal en estado INITIATED verificando contrato completo
        UUID vesselId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        String body = buildRequestBody(
                vesselId,
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                4,
                "08:30",
                "9600000.00",
                "Carlos Mendoza Gómez",
                "+573105558899",
                "carlos.mendoza@example.com"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.departure_time").value("08:30"))
                .andExpect(jsonPath("$.ttl_minutes").value(15))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(new BigDecimal(json.get("estimated_total").asText())).isEqualByComparingTo(new BigDecimal("9600000.00"));

        UUID reservationId = UUID.fromString(json.get("reservation_id").asText());
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/reservations/" + reservationId); // el 201 apunta al recurso creado
        Reservation persisted = reservationRepositoryPort.findById(reservationId).orElseThrow();
        assertThat(persisted.getHarborTimezone()).isEqualTo("America/Bogota");
        assertThat(persisted.getOwnerId()).isEqualTo(defaultOwnerId);
        assertThat(persisted.getStatus()).isEqualTo(ReservationStatus.INITIATED);

        String createdAtStr = json.get("created_at").asText();
        String ttlExpiresAtStr = json.get("ttl_expires_at").asText();
        assertThat(createdAtStr).endsWith("-05:00");
        assertThat(ttlExpiresAtStr).endsWith("-05:00");

        OffsetDateTime createdAt = OffsetDateTime.parse(createdAtStr);
        OffsetDateTime ttlExpiresAt = OffsetDateTime.parse(ttlExpiresAtStr);
        assertThat(Duration.between(createdAt, ttlExpiresAt)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void sameIdempotencyKeyReturnsSameReservation() throws Exception { // peticiones repetidas devuelven la misma reserva sin duplicar
        UUID vesselId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        String body = buildRequestBody(
                vesselId,
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                2,
                "08:30",
                "4500000.00",
                "Juan Perez",
                "+573001234567",
                null
        );

        MvcResult firstResult = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        assertThat(firstJson.get("reservation_id").asText()).isEqualTo(secondJson.get("reservation_id").asText());

        assertThat(reservationRepositoryPort.findByRenter(userId, 1, 10).totalItems()).isEqualTo(1);
    }

    @Test
    void rejectsPassengersOverCapacity() throws Exception { // rechaza reservas que excedan la capacidad maxima
        UUID vesselId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(defaultOwnerId, 4, "America/Bogota"));

        String body = buildRequestBody(
                vesselId,
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                5,
                "08:30",
                "5000000.00",
                "Ana Gomez",
                "+573119876543",
                null
        );

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));

        assertThat(reservationRepositoryPort.findByRenter(userId, 1, 10).totalItems()).isEqualTo(0);
    }

    @Test
    void rejectsOverlap() throws Exception { // rechaza reservas solapadas con estados bloqueantes e incluye el ultimo dia
        UUID vesselId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ReservationView blocking = createReservation(userId, vesselId, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12));
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                blocking.reservationId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                userId, null, null, null, null, null));
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                blocking.reservationId(), ReservationStatus.RESERVED, null, ActorType.RENTER, "Pago confirmado",
                userId, null, null, null, null, null));

        String overlappingKey = UUID.randomUUID().toString();
        String overlappingBody = buildRequestBody(
                vesselId,
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 14),
                2,
                "08:30",
                "3000000.00",
                "Pedro Picapiedra",
                "+573104445566",
                null
        );

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", overlappingKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VESSEL_NOT_AVAILABLE"));

        String freeKey = UUID.randomUUID().toString();
        String freeBody = buildRequestBody(
                vesselId,
                LocalDate.of(2027, 3, 13),
                LocalDate.of(2027, 3, 15),
                2,
                "08:30",
                "3000000.00",
                "Pedro Picapiedra",
                "+573104445566",
                null
        );

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", freeKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freeBody))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsEndBeforeStart() throws Exception { // rechaza solicitudes donde end_date sea anterior a start_date
        UUID vesselId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        String body = buildRequestBody(
                vesselId,
                LocalDate.of(2027, 3, 15),
                LocalDate.of(2027, 3, 10),
                2,
                "08:30",
                "2000000.00",
                "Mario Bros",
                "+573123334455",
                null
        );

        mockMvc.perform(post("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }
}
