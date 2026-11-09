package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
class CreateReservationOutboxIntegrationTest { // pruebas integradas de persistencia transaccional del evento outbox CU-02

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para invocar controller rest

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso para invocacion directa en rollback

    @Autowired
    private JdbcTemplate jdbcTemplate; // cliente jdbc directo para verificar tablas fisicas

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion del puerto de estado operativo

    @MockitoSpyBean
    private IdempotencyPort idempotencyPort; // espia de idempotencia para forzar fallo post-enqueue

    private final ObjectMapper objectMapper = new JsonMapper(); // mapeador jackson 3 para inspeccionar payload json
    private final Set<UUID> createdReservationIds = new HashSet<>(); // conjunto para rastreo de reservas creadas por el test
    private UUID defaultOwnerId; // id de propietario por defecto

    @BeforeEach
    void setUp() { // inicializa dependencias de flota
        defaultOwnerId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(defaultOwnerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    @AfterEach
    void cleanUp() { // limpia las filas de outbox y claves; las reservas ya no se pueden borrar (T-10)
        for (UUID resId : createdReservationIds) {
            jdbcTemplate.update("DELETE FROM outbox_message WHERE payload LIKE ?", "%" + resId + "%");
            // T-10: la creacion escribe en reservation_status_audit (FK) y el trigger de inmutabilidad
            // impide borrar la fila hija; no se elimina la reserva para no violar la FK de esa tabla.
        }
        createdReservationIds.clear();
        Mockito.reset(idempotencyPort);
    }

    private String buildRequestBody(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers) { // construye body json para controller
        return """
                {
                  "vessel_id": "%s",
                  "start_date": "%s",
                  "end_date": "%s",
                  "passengers": %d,
                  "departure_time": "08:30",
                  "estimated_total": "1200000.00",
                  "holder": {
                    "full_name": "Ana Perez",
                    "phone": "+573009876543",
                    "email": "ana@example.com"
                  }
                }
                """.formatted(vesselId, startDate, endDate, passengers);
    }

    @Test
    void createsOutboxMessageWithExactSevenFieldsAndBoatId() throws Exception { // caso a y d: una fila en outbox con boat_id, sin vessel_id y exactamente 7 campos
        UUID vesselId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        String body = buildRequestBody(vesselId, LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 12), 4);

        MvcResult result = mockMvc.perform(post("/api/v1/reservations") // invoca creacion via endpoint rest
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID reservationId = UUID.fromString(responseJson.get("reservation_id").asText());
        createdReservationIds.add(reservationId); // registra id para limpieza exclusiva en aftereach

        List<Map<String, Object>> outboxRows = jdbcTemplate.queryForList( // consulta filas en outbox_message
                "SELECT id, event_id, destination, exchange, routing_key, payload FROM outbox_message WHERE exchange = 'seashare.reservations' AND routing_key = 'reservation.info.provided'"
        );

        Map<String, Object> matchingRow = outboxRows.stream() // filtra mensaje correspondiente a la reserva
                .filter(row -> row.get("payload").toString().contains(reservationId.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No outbox row found for reservation " + reservationId));

        assertThat(matchingRow.get("exchange")).isEqualTo("seashare.reservations"); // verifica exchange topic
        assertThat(matchingRow.get("routing_key")).isEqualTo("reservation.info.provided"); // verifica routing key
        assertThat(matchingRow.get("destination")).isEqualTo("AMQP"); // verifica destino amqp

        UUID expectedEventId = UUID.nameUUIDFromBytes(("reservation.info.provided:" + reservationId).getBytes(StandardCharsets.UTF_8)); // calcula event_id deterministico
        assertThat(matchingRow.get("event_id")).isEqualTo(expectedEventId); // verifica event_id deterministico unico

        JsonNode payloadJson = objectMapper.readTree((String) matchingRow.get("payload")); // parsea json del payload con jackson 3
        assertThat(payloadJson.propertyNames()).containsExactlyInAnyOrder( // verifica exactamente los 7 campos requeridos por modulo 3
                "reservation_id", "boat_id", "start_date", "end_date", "passengers", "owner_id", "max_capacity"
        );

        assertThat(payloadJson.has("vessel_id")).isFalse(); // garantiza que no aparece vessel_id
        assertThat(payloadJson.has("departure_time")).isFalse(); // garantiza que no aparece departure_time
        assertThat(payloadJson.has("harbor_timezone")).isFalse(); // garantiza que no aparece harbor_timezone
        assertThat(payloadJson.has("estimated_total")).isFalse(); // garantiza que no aparecen montos

        assertThat(payloadJson.get("reservation_id").asText()).isEqualTo(reservationId.toString()); // verifica id de reserva
        assertThat(payloadJson.get("boat_id").asText()).isEqualTo(vesselId.toString()); // verifica que boat_id lleva el valor del vessel_id interno
        assertThat(payloadJson.get("start_date").asText()).isEqualTo("2027-06-10"); // verifica start_date
        assertThat(payloadJson.get("end_date").asText()).isEqualTo("2027-06-12"); // verifica end_date
        assertThat(payloadJson.get("start_date").asText()).matches("^\\d{4}-\\d{2}-\\d{2}$"); // verifica formato yyyy-MM-dd
        assertThat(payloadJson.get("end_date").asText()).matches("^\\d{4}-\\d{2}-\\d{2}$"); // verifica formato yyyy-MM-dd
        assertThat(payloadJson.get("passengers").asInt()).isEqualTo(4); // verifica pasajeros
        assertThat(payloadJson.get("owner_id").asText()).isEqualTo(defaultOwnerId.toString()); // verifica owner_id
        assertThat(payloadJson.get("max_capacity").asInt()).isEqualTo(10); // verifica max_capacity
    }

    @Test
    void idempotentRepeatDoesNotEnqueueAdditionalMessage() throws Exception { // caso b y c: repeticion idempotente no encola segundo mensaje y conserva un unico message-id
        UUID vesselId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        String body = buildRequestBody(vesselId, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 3), 2);

        MvcResult firstResult = mockMvc.perform(post("/api/v1/reservations") // primera peticion
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        UUID reservationId = UUID.fromString(firstJson.get("reservation_id").asText());
        createdReservationIds.add(reservationId); // registra id para limpieza en aftereach

        MvcResult secondResult = mockMvc.perform(post("/api/v1/reservations") // segunda peticion con misma clave
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        assertThat(secondJson.get("reservation_id").asText()).isEqualTo(reservationId.toString()); // devuelve misma reserva

        Integer count = jdbcTemplate.queryForObject( // cuenta mensajes en outbox para esta reserva
                "SELECT COUNT(*) FROM outbox_message WHERE payload LIKE ?",
                Integer.class,
                "%" + reservationId + "%"
        );
        assertThat(count).isEqualTo(1); // exactamente una sola fila, ninguna fila adicional en repeticion
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void forcedFailureAfterEnqueueRollsBackReservationAndOutbox() { // caso b y regla 3: fallo tras enqueue revierte reserva y outbox
        UUID vesselId = UUID.randomUUID();
        UUID renterId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        Integer outboxBefore = jdbcTemplate.queryForObject( // cuenta filas de outbox_message antes del intento
                "SELECT COUNT(*) FROM outbox_message WHERE routing_key = 'reservation.info.provided'",
                Integer.class
        );

        doThrow(new RuntimeException("falla simulada tras enqueue")) // fuerza fallo inmediatamente despues del enqueue en complete
                .when(idempotencyPort).complete(any(), any(), any());

        CreateReservationCommand command = new CreateReservationCommand(
                renterId,
                idempotencyKey,
                vesselId,
                LocalDate.of(2027, 8, 1),
                LocalDate.of(2027, 8, 3),
                2,
                "Usuario Rollback",
                "+573001112233",
                "rollback@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("1500000.00")
        );

        assertThatThrownBy(() -> createReservationUseCase.execute(command)) // ejecuta caso de uso esperando excepcion
                .isInstanceOf(RuntimeException.class)
                .hasMessage("falla simulada tras enqueue");

        Integer reservationsCount = jdbcTemplate.queryForObject( // verifica que la reserva fue revertida de base de datos
                "SELECT COUNT(*) FROM reservation WHERE renter_id = ?",
                Integer.class,
                renterId
        );
        assertThat(reservationsCount).isEqualTo(0); // 0 filas en reservation

        Integer outboxAfter = jdbcTemplate.queryForObject( // cuenta filas de outbox_message despues del rollback
                "SELECT COUNT(*) FROM outbox_message WHERE routing_key = 'reservation.info.provided'",
                Integer.class
        );
        assertThat(outboxAfter).isEqualTo(outboxBefore); // garantiza que no quedo ninguna fila en outbox tras el rollback
    }
}
