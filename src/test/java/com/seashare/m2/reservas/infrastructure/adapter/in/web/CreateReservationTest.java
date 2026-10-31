package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.port.out.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.infrastructure.adapter.out.persistence.ReservationEntity;
import com.seashare.m2.reservas.infrastructure.adapter.out.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateReservationTest { // pruebas funcionales del caso de uso iniciar reserva CU-02

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para realizar peticiones rest

    @Autowired
    private ReservationJpaRepository reservationJpaRepository; // repositorio jpa para aserciones y datos iniciales

    @Autowired
    private JdbcTemplate jdbcTemplate; // cliente jdbc para limpieza y verificaciones de conteo

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de informacion de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion del puerto de estado operativo

    private final ObjectMapper objectMapper = new ObjectMapper(); // serializador json para analizar respuestas
    private final List<UUID> trackedVessels = new ArrayList<>(); // identificadores de embarcaciones creadas en la prueba
    private final List<String> trackedKeys = new ArrayList<>(); // claves de idempotencia usadas en la prueba
    private UUID defaultOwnerId; // identificador del propietario para mocks por defecto

    @BeforeEach
    void setUp() { // inicializa mocks de modulo 1 antes de cada prueba
        defaultOwnerId = UUID.randomUUID(); // id unico para el propietario mockeado
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(defaultOwnerId, 12, "America/Bogota")); // mock exitoso por defecto
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true); // embarcacion disponible por defecto
    }

    @AfterEach
    void tearDown() { // limpieza con jdbc sin tocar triggers ni truncar tablas
        for (UUID vesselId : trackedVessels) { // itera embarcaciones registradas en las pruebas
            jdbcTemplate.update("DELETE FROM reservation WHERE vessel_id = ?", vesselId); // elimina reservas de la embarcacion
        }
        for (String key : trackedKeys) { // itera claves registradas en las pruebas
            jdbcTemplate.update("DELETE FROM idempotency_key WHERE key = ?", key); // elimina registros de idempotencia
        }
        trackedVessels.clear(); // vacia lista de embarcaciones
        trackedKeys.clear(); // vacia lista de claves
    }

    private String buildRequestBody(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers,
                                    String departureTime, String estimatedTotal, String name, String phone, String email) { // genera json snake_case
        String emailField = email != null ? ",\"email\":\"" + email + "\"" : ""; // campo email condicional
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
                """.formatted(vesselId, startDate, endDate, passengers, departureTime, estimatedTotal, name, phone, emailField); // json interpolado
    }

    @Test
    void createsReservationInInitiated() throws Exception { // crea reserva formal en estado INITIATED verificando contrato completo
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String idempotencyKey = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(idempotencyKey); // registra para limpieza
        UUID userId = UUID.randomUUID(); // id de usuario arrendatario

        String body = buildRequestBody( // construye payload valido
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

        MvcResult result = mockMvc.perform(post("/api/v1/reservations") // ejecuta peticion post
                        .header("X-User-Id", userId.toString()) // header de identidad
                        .header("X-User-Role", "RENTER") // header de rol
                        .header("Idempotency-Key", idempotencyKey) // header de idempotencia
                        .contentType(MediaType.APPLICATION_JSON) // tipo de contenido json
                        .content(body)) // payload json
                .andExpect(status().isCreated()) // valida codigo http 201
                .andExpect(jsonPath("$.status").value("INITIATED")) // valida estado INITIATED
                .andExpect(jsonPath("$.currency").value("COP")) // valida moneda COP
                .andExpect(jsonPath("$.departure_time").value("08:30")) // valida hora zarpe 08:30
                .andExpect(jsonPath("$.ttl_minutes").value(15)) // valida ttl 15 minutos
                .andReturn(); // captura resultado mvc

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()); // parsea arbol json
        String estimatedTotalStr = json.get("estimated_total").asText(); // extrae estimated_total
        assertThat(new BigDecimal(estimatedTotalStr)).isEqualByComparingTo(new BigDecimal("9600000.00")); // compara valor numerico

        UUID reservationId = UUID.fromString(json.get("reservation_id").asText()); // extrae reservation_id
        ReservationEntity inDb = reservationJpaRepository.findById(reservationId).orElseThrow(); // consulta entidad persistida
        assertThat(inDb.getHarborTimezone()).isEqualTo("America/Bogota"); // valida zona horaria en base de datos
        assertThat(inDb.getOwnerId()).isEqualTo(defaultOwnerId); // valida propietario obtenido del mock

        String createdAtStr = json.get("created_at").asText(); // extrae texto de creacion
        String ttlExpiresAtStr = json.get("ttl_expires_at").asText(); // extrae texto de expiracion
        assertThat(createdAtStr).endsWith("-05:00"); // comprueba offset horario bogota en creacion
        assertThat(ttlExpiresAtStr).endsWith("-05:00"); // comprueba offset horario bogota en expiracion

        OffsetDateTime createdAt = OffsetDateTime.parse(createdAtStr); // parsea fecha de creacion
        OffsetDateTime ttlExpiresAt = OffsetDateTime.parse(ttlExpiresAtStr); // parsea fecha de expiracion
        assertThat(Duration.between(createdAt, ttlExpiresAt)).isEqualTo(Duration.ofMinutes(15)); // valida diferencia exacta de quince minutos
    }

    @Test
    void sameIdempotencyKeyReturnsSameReservation() throws Exception { // peticiones repetidas devuelven la misma reserva sin duplicar filas
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String idempotencyKey = UUID.randomUUID().toString(); // clave de idempotencia compartida
        trackedKeys.add(idempotencyKey); // registra para limpieza
        UUID userId = UUID.randomUUID(); // id de usuario

        String body = buildRequestBody( // cuerpo valido
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

        MvcResult firstResult = mockMvc.perform(post("/api/v1/reservations") // primer envio
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()) // valida 201 en primera llamada
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/v1/reservations") // segundo envio con misma clave
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()) // valida 201 en segunda llamada
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString()); // json primer envio
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString()); // json segundo envio
        assertThat(firstJson.get("reservation_id").asText()).isEqualTo(secondJson.get("reservation_id").asText()); // valida mismo id

        Integer count = jdbcTemplate.queryForObject( // cuenta filas de reservas creadas
                "SELECT count(*) FROM reservation WHERE vessel_id = ?",
                Integer.class,
                vesselId
        );
        assertThat(count).isEqualTo(1); // comprueba existencia de una sola fila
    }

    @Test
    void rejectsPassengersOverCapacity() throws Exception { // rechaza reservas que excedan la capacidad maxima de la embarcacion
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String idempotencyKey = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(idempotencyKey); // registra para limpieza
        UUID userId = UUID.randomUUID(); // id de usuario

        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(defaultOwnerId, 4, "America/Bogota")); // capacidad fija de 4

        String body = buildRequestBody( // solicita 5 pasajeros superando capacidad
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

        mockMvc.perform(post("/api/v1/reservations") // ejecuta peticion post
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict()) // valida codigo http 409
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED")); // valida codigo CAPACITY_EXCEEDED

        Integer count = jdbcTemplate.queryForObject( // cuenta filas en base de datos
                "SELECT count(*) FROM reservation WHERE vessel_id = ?",
                Integer.class,
                vesselId
        );
        assertThat(count).isEqualTo(0); // verifica que no se creo ninguna fila
    }

    @Test
    void rejectsOverlap() throws Exception { // rechaza reservas solapadas con estados bloqueantes e incluye el ultimo dia
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        UUID userId = UUID.randomUUID(); // id de usuario

        ReservationEntity blocking = new ReservationEntity(); // prepara entidad bloqueante
        blocking.setReservationCode("RS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()); // codigo unico
        blocking.setRenterId(UUID.randomUUID()); // arrendatario
        blocking.setVesselId(vesselId); // misma embarcacion
        blocking.setOwnerId(defaultOwnerId); // propietario
        blocking.setStatus(ReservationStatus.RESERVED); // estado RESERVED que retiene fechas
        blocking.setStartDate(LocalDate.of(2027, 3, 10)); // inicio dia 10
        blocking.setEndDate(LocalDate.of(2027, 3, 12)); // fin dia 12
        blocking.setDepartureTime(LocalTime.of(8, 30)); // hora zarpe
        blocking.setPassengers(2); // pasajeros
        blocking.setHolderName("Reserva Previa"); // titular
        blocking.setHolderPhone("+573009998877"); // telefono
        blocking.setCurrency("COP"); // moneda
        blocking.setM1Blocked(true); // indicador m1
        blocking.setCreatedAt(Instant.now()); // creacion
        blocking.setHarborTimezone("America/Bogota"); // zona horaria
        reservationJpaRepository.saveAndFlush(blocking); // persiste reserva bloqueante

        String overlappingKey = UUID.randomUUID().toString(); // clave para solicitud solapada
        trackedKeys.add(overlappingKey); // registra para limpieza
        String overlappingBody = buildRequestBody( // rango que inicia el dia 12 solapando el final
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

        mockMvc.perform(post("/api/v1/reservations") // intento con solapamiento en dia 12
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", overlappingKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBody))
                .andExpect(status().isConflict()) // valida 409 conflict
                .andExpect(jsonPath("$.code").value("VESSEL_NOT_AVAILABLE")); // valida codigo VESSEL_NOT_AVAILABLE

        String freeKey = UUID.randomUUID().toString(); // clave para solicitud libre
        trackedKeys.add(freeKey); // registra para limpieza
        String freeBody = buildRequestBody( // rango libre del dia 13 al 15
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

        mockMvc.perform(post("/api/v1/reservations") // intento sin solapamiento
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", freeKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freeBody))
                .andExpect(status().isCreated()); // valida 201 created
    }

    @Test
    void rejectsEndBeforeStart() throws Exception { // rechaza solicitudes donde end_date sea anterior a start_date
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String idempotencyKey = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(idempotencyKey); // registra para limpieza
        UUID userId = UUID.randomUUID(); // id de usuario

        String body = buildRequestBody( // fechas invertidas
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

        mockMvc.perform(post("/api/v1/reservations") // envio con fechas inconsistentes
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest()) // valida 400 bad request
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida codigo INVALID_PARAMETERS
    }
}
