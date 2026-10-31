package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.port.out.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateReservationValidationTest { // pruebas de validacion, errores y concurrencia para iniciar reserva

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para pruebas de controladores

    @Autowired
    private JdbcTemplate jdbcTemplate; // cliente jdbc para aserciones y limpieza

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // mock de puerto de consulta de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // mock de puerto de estado operativo

    private final ObjectMapper objectMapper = new ObjectMapper(); // serializador json
    private final List<UUID> trackedVessels = Collections.synchronizedList(new ArrayList<>()); // embarcaciones para limpieza concurrente
    private final List<String> trackedKeys = Collections.synchronizedList(new ArrayList<>()); // claves para limpieza concurrente
    private UUID defaultOwnerId; // id de propietario por defecto

    @BeforeEach
    void setUp() { // inicializacion de mocks antes de cada prueba
        defaultOwnerId = UUID.randomUUID(); // genera owner id unico
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(defaultOwnerId, 12, "America/Bogota")); // mock exitoso
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true); // disponible por defecto
    }

    @AfterEach
    void tearDown() { // limpieza con jdbc sin alterar triggers
        for (UUID vesselId : trackedVessels) { // elimina reservas creadas
            jdbcTemplate.update("DELETE FROM reservation WHERE vessel_id = ?", vesselId); // borra por embarcacion
        }
        for (String key : trackedKeys) { // elimina claves de idempotencia usadas
            jdbcTemplate.update("DELETE FROM idempotency_key WHERE key = ?", key); // borra por clave
        }
        trackedVessels.clear(); // limpia lista
        trackedKeys.clear(); // limpia lista
    }

    private String buildValidBody(UUID vesselId) { // genera cuerpo json valido generico
        return """
                {
                  "vessel_id": "%s",
                  "start_date": "2027-03-10",
                  "end_date": "2027-03-12",
                  "passengers": 2,
                  "departure_time": "08:30",
                  "estimated_total": "9600000.00",
                  "holder": {
                    "full_name": "Carlos Gomez",
                    "phone": "+573001234567"
                  }
                }
                """.formatted(vesselId); // json formateado
    }

    @Test
    void missingIdempotencyKeyIs400() throws Exception { // omision de cabecera Idempotency-Key retorna 400 INVALID_PARAMETERS
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza

        mockMvc.perform(post("/api/v1/reservations") // peticion sin header Idempotency-Key
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isBadRequest()) // valida 400
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida INVALID_PARAMETERS
    }

    @Test
    void invalidIdempotencyKeyIs400InvalidUuid() throws Exception { // cabecera Idempotency-Key con formato no uuid retorna 400 INVALID_UUID
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza

        mockMvc.perform(post("/api/v1/reservations") // peticion con clave no uuid
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", "not-a-valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isBadRequest()) // valida 400
                .andExpect(jsonPath("$.code").value("INVALID_UUID")); // valida INVALID_UUID
    }

    @Test
    void missingUserIdIs401() throws Exception { // omision de X-User-Id retorna 401 UNAUTHENTICATED antes de procesar el cuerpo
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza

        mockMvc.perform(post("/api/v1/reservations") // peticion sin X-User-Id
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isUnauthorized()) // valida 401
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED")); // valida UNAUTHENTICATED
    }

    @Test
    void ownerRoleIs403() throws Exception { // llamada con rol OWNER retorna 403 PROFILE_NOT_AUTHORIZED
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza

        mockMvc.perform(post("/api/v1/reservations") // peticion con rol OWNER
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "OWNER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isForbidden()) // valida 403
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED")); // valida PROFILE_NOT_AUTHORIZED
    }

    @Test
    void fleetUnavailableIs503WritesNothing() throws Exception { // falla en modulo 1 retorna 503 y revierte reserva y clave
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza

        when(fleetInfoPort.getVesselInfo(vesselId)) // simula caida del servicio de flota
                .thenThrow(new ReservationRuleException(ErrorCode.FLEET_SERVICE_UNAVAILABLE, "Fleet service is unavailable"));

        mockMvc.perform(post("/api/v1/reservations") // peticion que falla por servicio caido
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isServiceUnavailable()) // valida 503
                .andExpect(jsonPath("$.code").value("FLEET_SERVICE_UNAVAILABLE")); // valida FLEET_SERVICE_UNAVAILABLE

        Integer reservationCount = jdbcTemplate.queryForObject( // verifica rollback en reservation
                "SELECT count(*) FROM reservation WHERE vessel_id = ?",
                Integer.class,
                vesselId
        );
        assertThat(reservationCount).isEqualTo(0); // cero reservas persistidas

        Integer keyCount = jdbcTemplate.queryForObject( // verifica rollback en idempotency_key
                "SELECT count(*) FROM idempotency_key WHERE key = ?",
                Integer.class,
                key
        );
        assertThat(keyCount).isEqualTo(0); // cero claves persistidas
    }

    @Test
    void invalidHarborTimezoneIs503WritesNothing() throws Exception { // zona horaria ausente o invalida retorna 503 y revierte datos
        List<String> invalidZones = Arrays.asList("Not/AZone", "", null); // casos no validos segun especificacion
        for (String invalidZone : invalidZones) { // itera cada caso invalido
            UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
            trackedVessels.add(vesselId); // registra para limpieza
            String key = UUID.randomUUID().toString(); // clave de idempotencia unica
            trackedKeys.add(key); // registra para limpieza

            when(fleetInfoPort.getVesselInfo(vesselId)) // mockea zona horaria no valida
                    .thenReturn(new FleetInfo(defaultOwnerId, 12, invalidZone));

            mockMvc.perform(post("/api/v1/reservations") // ejecuta peticion post
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "RENTER")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildValidBody(vesselId)))
                    .andExpect(status().isServiceUnavailable()) // valida 503
                    .andExpect(jsonPath("$.code").value("HARBOR_TIMEZONE_UNAVAILABLE")); // valida HARBOR_TIMEZONE_UNAVAILABLE

            Integer reservationCount = jdbcTemplate.queryForObject( // valida rollback en reservation
                    "SELECT count(*) FROM reservation WHERE vessel_id = ?",
                    Integer.class,
                    vesselId
            );
            assertThat(reservationCount).isEqualTo(0); // cero filas en reservation

            Integer keyCount = jdbcTemplate.queryForObject( // valida rollback en idempotency_key
                    "SELECT count(*) FROM idempotency_key WHERE key = ?",
                    Integer.class,
                    key
            );
            assertThat(keyCount).isEqualTo(0); // cero filas en idempotency_key
        }
    }

    @Test
    void vesselNotFoundIs404() throws Exception { // embarcacion inexistente en flota retorna 404 VESSEL_NOT_FOUND
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza

        when(fleetInfoPort.getVesselInfo(vesselId)) // simula embarcacion no registrada
                .thenThrow(new ReservationRuleException(ErrorCode.VESSEL_NOT_FOUND, "Vessel not found in fleet catalog"));

        mockMvc.perform(post("/api/v1/reservations") // peticion con embarcacion no encontrada
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isNotFound()) // valida 404
                .andExpect(jsonPath("$.code").value("VESSEL_NOT_FOUND")); // valida VESSEL_NOT_FOUND
    }

    @Test
    void vesselNotAvailableIs409() throws Exception { // estado operativo no disponible retorna 409 VESSEL_NOT_AVAILABLE
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza

        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(false); // simula embarcacion inhabilitada

        mockMvc.perform(post("/api/v1/reservations") // peticion con embarcacion inoperativa
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isConflict()) // valida 409
                .andExpect(jsonPath("$.code").value("VESSEL_NOT_AVAILABLE")); // valida VESSEL_NOT_AVAILABLE
    }

    @Test
    void malformedJsonIs400() throws Exception { // json con sintaxis corrupta retorna 400 INVALID_PARAMETERS
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza

        mockMvc.perform(post("/api/v1/reservations") // peticion con json truncado
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"vessel_id\": ")) // json malformado
                .andExpect(status().isBadRequest()) // valida 400
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida INVALID_PARAMETERS
    }

    @Test
    void invalidEstimatedTotalIs400() throws Exception { // montos con formato invalido son rechazados con 400 INVALID_PARAMETERS
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza

        List<String> invalidTotals = List.of("abc", "-5", "1.23456", "1e5"); // casos invalidos segun regex
        for (String invalidTotal : invalidTotals) { // evalua cada caso no permitido
            String key = UUID.randomUUID().toString(); // clave unica para cada iteracion
            trackedKeys.add(key); // registra para limpieza

            String body = """
                    {
                      "vessel_id": "%s",
                      "start_date": "2027-03-10",
                      "end_date": "2027-03-12",
                      "passengers": 2,
                      "departure_time": "08:30",
                      "estimated_total": "%s",
                      "holder": {
                        "full_name": "Carlos Gomez",
                        "phone": "+573001234567"
                      }
                    }
                    """.formatted(vesselId, invalidTotal); // json con monto invalido

            mockMvc.perform(post("/api/v1/reservations") // envio con total no conforme
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "RENTER")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest()) // valida 400
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida INVALID_PARAMETERS
        }
    }

    @Test
    void expiredIdempotencyKeyCreatesNewReservation() throws Exception { // clave vencida despues de 60 segundos reutiliza la fila y genera nueva reserva
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave de idempotencia
        trackedKeys.add(key); // registra para limpieza
        UUID oldReservationId = UUID.randomUUID(); // id de reserva previo simulado

        OffsetDateTime expiredTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(61); // timestamp de hace 61 segundos
        jdbcTemplate.update( // inserta clave expirada manualmente
                "INSERT INTO idempotency_key (key, endpoint, response_ref, created_at) VALUES (?, ?, ?, ?)",
                key,
                "CREATE_RESERVATION",
                oldReservationId.toString(),
                expiredTimestamp
        );

        MvcResult result = mockMvc.perform(post("/api/v1/reservations") // peticion con clave vencida
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildValidBody(vesselId)))
                .andExpect(status().isCreated()) // valida 201 created
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()); // parsea respuesta
        UUID newReservationId = UUID.fromString(json.get("reservation_id").asText()); // extrae nuevo reservation_id

        assertThat(newReservationId).isNotEqualTo(oldReservationId); // confirma que no reutilizo el id viejo

        String updatedRef = jdbcTemplate.queryForObject( // consulta referencia actualizada en base de datos
                "SELECT response_ref FROM idempotency_key WHERE key = ? AND endpoint = ?",
                String.class,
                key,
                "CREATE_RESERVATION"
        );
        assertThat(updatedRef).isEqualTo(newReservationId.toString()); // verifica asociacion con el nuevo id
    }

    @Test
    void concurrentRequestsWithSameKeyCreateOneReservation() throws Exception { // solicitudes concurrentes con misma clave generan solo una reserva
        UUID vesselId = UUID.randomUUID(); // embarcacion aleatoria
        trackedVessels.add(vesselId); // registra para limpieza
        String key = UUID.randomUUID().toString(); // clave compartida
        trackedKeys.add(key); // registra para limpieza
        UUID userId = UUID.randomUUID(); // id de usuario
        String body = buildValidBody(vesselId); // cuerpo valido

        int threadCount = 5; // cinco peticiones paralelas
        ExecutorService executor = Executors.newFixedThreadPool(threadCount); // pool de hilos
        CountDownLatch readyLatch = new CountDownLatch(threadCount); // sincronizacion de arranque conjunto
        CountDownLatch startLatch = new CountDownLatch(1); // disparo simultaneo

        List<Future<MvcResult>> futures = new ArrayList<>(); // lista de futuros
        for (int i = 0; i < threadCount; i++) { // encola llamadas concurrentes
            futures.add(executor.submit(() -> {
                readyLatch.countDown(); // notifica que el hilo esta listo
                startLatch.await(); // espera la senal de disparo
                return mockMvc.perform(post("/api/v1/reservations") // ejecuta peticion concorrente
                                .header("X-User-Id", userId.toString())
                                .header("X-User-Role", "RENTER")
                                .header("Idempotency-Key", key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn(); // retorna resultado
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS); // espera a que los 5 hilos esten listos
        startLatch.countDown(); // libera la ejecucion simultanea

        List<String> reservationIds = new ArrayList<>(); // lista de identificadores obtenidos
        for (Future<MvcResult> future : futures) { // recupera resultados
            MvcResult mvcResult = future.get(10, TimeUnit.SECONDS); // obtiene resultado con timeout
            assertThat(mvcResult.getResponse().getStatus()).isEqualTo(201); // valida status 201 en todas
            JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString()); // lee json
            reservationIds.add(json.get("reservation_id").asText()); // acumula reservation_id
        }

        executor.shutdown(); // cierra executor

        String firstId = reservationIds.get(0); // toma id de la primera respuesta
        assertThat(reservationIds).containsOnly(firstId); // comprueba que todas recibieron el mismo reservation_id

        Integer rowCount = jdbcTemplate.queryForObject( // cuenta filas de reservas creadas
                "SELECT count(*) FROM reservation WHERE vessel_id = ?",
                Integer.class,
                vesselId
        );
        assertThat(rowCount).isEqualTo(1); // verifica que solo existe una unica reserva
    }
}
