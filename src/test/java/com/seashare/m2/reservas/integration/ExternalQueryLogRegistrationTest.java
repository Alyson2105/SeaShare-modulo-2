package com.seashare.m2.reservas.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.ExternalQueryLogEntry;
import com.seashare.m2.reservas.application.dto.ExternalQueryResult;
import com.seashare.m2.reservas.application.dto.ExternalQueryType;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.port.out.ExternalQueryLogPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.infrastructure.adapter.out.fleet.FleetHttpConfig;
import com.seashare.m2.reservas.infrastructure.adapter.out.fleet.FleetProperties;
import com.seashare.m2.reservas.infrastructure.adapter.out.fleet.HttpFleetInfoAdapter;
import com.seashare.m2.reservas.infrastructure.adapter.out.fleet.HttpFleetOperationalStatusAdapter;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = { // levanta los adaptadores HTTP reales
        "seashare.fleet.mode=http",
        "spring.datasource.hikari.maximum-pool-size=2" // pocas conexiones: la suite comparte max_connections del contenedor
})
@Import(PostgresTestContainers.class) // postgres en contenedor, patron de la suite
class ExternalQueryLogRegistrationTest { // registro de consultas a M1 en external_query_log (CU-09 FR-010, CU-10 FR-009)

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // id fijo de prueba

    private static final UUID OWNER_ID = UUID.fromString("a1c2e3f4-5678-90ab-cdef-1234567890ab"); // propietario fijo

    private static final String VESSEL_PATH = "/api/v1/vessels/" + VESSEL_ID; // ruta de la ficha

    private static final String STATUS_PATH = VESSEL_PATH + "/operational-status"; // ruta del estado operativo

    private static final WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()); // M1 falso unico

    static { // arranca una sola vez para toda la clase
        wireMock.start(); // servidor falso
        warmUp(); // la primera respuesta del servidor nuevo tarda mas de 300 ms
    }

    @DynamicPropertySource
    static void fleetBaseUrl(DynamicPropertyRegistry registry) { // apunta los adaptadores al WireMock
        registry.add("seashare.fleet.base-url", () -> "http://localhost:" + wireMock.port()); // url base dinamica
    }

    @AfterAll
    static void stopWireMock() { // detiene el servidor al terminar la clase
        wireMock.stop(); // libera el puerto
    }

    @Autowired
    private HttpFleetInfoAdapter infoAdapter; // adaptador de ficha bajo prueba

    @Autowired
    private HttpFleetOperationalStatusAdapter statusAdapter; // adaptador de estado bajo prueba

    @Autowired
    private ExternalQueryLogPort externalQueryLogPort; // puerto real de registro (transaccion propia)

    @Autowired
    private JdbcTemplate jdbcTemplate; // acceso crudo a las filas del registro

    @Autowired
    private PlatformTransactionManager transactionManager; // gestor para el test de rollback

    @BeforeEach
    void resetState() { // limpia stubs y filas antes de cada caso
        wireMock.resetAll(); // quita stubs del caso anterior
        jdbcTemplate.update("DELETE FROM external_query_log"); // registro limpio por caso
        MDC.clear(); // sin correlation id residual
    }

    @Test
    void vesselInfoOkRegistersOkRow() { // 200 util -> una fila OK con returned_status null
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(200) // stub de la ficha
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Bogota"))));

        FleetInfo info = infoAdapter.getVesselInfo(VESSEL_ID); // consulta real

        assertThat(info.maxCapacity()).isEqualTo(12); // el resultado no cambia
        Map<String, Object> row = singleRow(); // una sola fila
        assertThat(row.get("type")).isEqualTo("VESSEL_INFO"); // tipo CU-09
        assertThat(row.get("result")).isEqualTo("OK"); // resultado OK
        assertThat(row.get("returned_status")).isNull(); // en VESSEL_INFO no hay estado crudo
        assertThat(row.get("vessel_id")).isEqualTo(VESSEL_ID); // embarcacion consultada
        assertThat(row.get("consulted_at")).isNotNull(); // hora escrita desde ClockPort
    }

    @Test
    void vesselInfoTimeoutRegistersTimeoutRow() { // timeout en ambos intentos -> una fila TIMEOUT
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(200) // demora mayor al read timeout
                .withFixedDelay(800)
                .withBody(vesselBody("America/Bogota"))));

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corta por timeout
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo de siempre

        Map<String, Object> row = singleRow(); // una sola fila por consulta
        assertThat(row.get("result")).isEqualTo("TIMEOUT"); // resultado TIMEOUT
        assertThat(row.get("type")).isEqualTo("VESSEL_INFO"); // tipo CU-09
    }

    @Test
    void vesselInfoNotFoundRegistersErrorRow() { // 404 -> ERROR con el error original de la parte A
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(404))); // sin cuerpo

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corta en la primera llamada
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("ERROR"); // 4xx es ERROR
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // sin reintento
    }

    @Test
    void vesselServerError500RegistersErrorRow() { // 500 no se reintenta -> ERROR
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(500))); // fallo del servidor

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // error inmediato
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("ERROR"); // 5xx distinto de 503 es ERROR
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // sin reintento
    }

    @Test
    void vesselRedirect302RegistersErrorRow() { // 3xx -> ERROR
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(302))); // sin Location no se sigue

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // estado inesperado
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("ERROR"); // 3xx es ERROR
    }

    @Test
    void vesselInfo503PersistRegistersTimeoutRow() { // 503 tras el reintento -> TIMEOUT con un reintento
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(503))); // siempre transitorio

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // se agota el reintento
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("TIMEOUT"); // 503 persistente es TIMEOUT
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // un reintento y corte
    }

    @Test
    void vesselInfoRetryThenOkRegistersSingleOkRow() { // 503 y luego 200 -> una sola fila OK
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).inScenario("retry") // primer intento transitorio
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("SECOND"));
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).inScenario("retry") // segundo intento exitoso
                .whenScenarioStateIs("SECOND")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(vesselBody("America/Bogota"))));

        FleetInfo info = infoAdapter.getVesselInfo(VESSEL_ID); // reintento exitoso

        assertThat(info.harborTimezone()).isEqualTo("America/Bogota"); // el resultado de la parte A no cambia
        Map<String, Object> row = singleRow(); // una sola fila
        assertThat(row.get("result")).isEqualTo("OK"); // solo se registra el resultado final
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // dos intentos
    }

    @Test
    void vesselInvalidTimezoneRegistersErrorRow() { // timezone invalida -> ERROR
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(200) // cuerpo con zona mala
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Nope"))));

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corte de siempre
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("ERROR"); // timezone invalida es ERROR
    }

    @Test
    void statusAvailableRegistersOkWithRawStatus() { // 200 AVAILABLE -> true y fila OK con el valor crudo
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // estado disponible
                .withHeader("Content-Type", "application/json")
                .withBody(statusBody("AVAILABLE"))));

        assertThat(statusAdapter.isAvailable(VESSEL_ID)).isTrue(); // el resultado no cambia

        Map<String, Object> row = singleRow(); // una sola fila
        assertThat(row.get("type")).isEqualTo("OPERATIONAL_STATUS"); // tipo CU-10
        assertThat(row.get("result")).isEqualTo("OK"); // resultado OK
        assertThat(row.get("returned_status")).isEqualTo("AVAILABLE"); // valor crudo de M1
    }

    @Test
    void statusUnknownRawStatusRegistersOkRow() { // estado desconocido -> false pero OK con el valor crudo
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // estado raro
                .withHeader("Content-Type", "application/json")
                .withBody(statusBody("WEIRD_STATE"))));

        assertThat(statusAdapter.isAvailable(VESSEL_ID)).isFalse(); // solo AVAILABLE es true

        Map<String, Object> row = singleRow(); // una sola fila
        assertThat(row.get("result")).isEqualTo("OK"); // 200 util es OK
        assertThat(row.get("returned_status")).isEqualTo("WEIRD_STATE"); // crudo sin validar
    }

    @Test
    void statusTimeoutRegistersTimeoutRow() { // timeout en los dos intentos -> una fila TIMEOUT
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // demora mayor al read timeout
                .withFixedDelay(800)
                .withBody(statusBody("AVAILABLE"))));

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID)) // corta por timeout
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("TIMEOUT"); // resultado TIMEOUT
    }

    @Test
    void statusNotFoundRegistersErrorRow() { // 404 -> ERROR con el error original
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(404))); // sin cuerpo

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID)) // corta en la primera llamada
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode())); // codigo de siempre

        assertThat(singleRow().get("result")).isEqualTo("ERROR"); // 4xx es ERROR
    }

    @Test
    void statusEmptyBodyThrowsAndRegistersSingleErrorRow() { // 200 sin cuerpo -> FLEET_SERVICE_UNAVAILABLE y una fila ERROR
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200))); // cuerpo vacio

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID)) // corte de siempre
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo esperado

        Map<String, Object> row = singleRow(); // una sola fila por consulta
        assertThat(row.get("result")).isEqualTo("ERROR"); // sin cuerpo util es ERROR
        assertThat(row.get("returned_status")).isNull(); // sin estado crudo
    }

    @Test
    void statusInvalidJsonThrowsAndRegistersSingleErrorRow() { // 200 ilegible -> FLEET_SERVICE_UNAVAILABLE y una fila ERROR
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // json roto
                .withHeader("Content-Type", "application/json")
                .withBody("{ not-json")));

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID)) // corte de siempre
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo esperado

        Map<String, Object> row = singleRow(); // una sola fila por consulta
        assertThat(row.get("result")).isEqualTo("ERROR"); // cuerpo ilegible es ERROR
        assertThat(row.get("returned_status")).isNull(); // sin estado crudo
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM external_query_log", Integer.class)) // conteo explicito
                .isEqualTo(1); // la fila del registro es unica
    }

    @Test
    void statusMissingFieldThrowsAndRegistersSingleErrorRow() { // 200 sin operational_status -> FLEET_SERVICE_UNAVAILABLE y una fila ERROR
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // cuerpo sin el campo
                .withHeader("Content-Type", "application/json")
                .withBody("{\"vessel_id\":\"" + VESSEL_ID + "\"}")));

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID)) // corte de siempre
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo esperado

        Map<String, Object> row = singleRow(); // una sola fila por consulta
        assertThat(row.get("result")).isEqualTo("ERROR"); // sin campo es ERROR
        assertThat(row.get("returned_status")).isNull(); // sin estado crudo
    }

    @Test
    void correlationIdTakenFromMdc() { // el correlation id sale del MDC de la peticion
        MDC.put("correlationId", "corr-test-123"); // contexto HTTP simulado
        try {
            wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(200) // stub de la ficha
                    .withHeader("Content-Type", "application/json")
                    .withBody(vesselBody("America/Bogota"))));

            infoAdapter.getVesselInfo(VESSEL_ID); // consulta con MDC activo
        } finally {
            MDC.remove("correlationId"); // limpia el MDC
        }

        assertThat(singleRow().get("correlation_id")).isEqualTo("corr-test-123"); // valor leido del MDC
    }

    @Test
    void longValuesAreTruncatedToColumnLimits() { // returned_status a 30 y correlation_id a 64
        MDC.put("correlationId", "c".repeat(80)); // correlation id largo demas
        try {
            wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // estado largo demas
                    .withHeader("Content-Type", "application/json")
                    .withBody(statusBody("S".repeat(50)))));

            statusAdapter.isAvailable(VESSEL_ID); // registro con valores largos
        } finally {
            MDC.remove("correlationId"); // limpia el MDC
        }

        Map<String, Object> row = singleRow(); // una sola fila
        assertThat(row.get("returned_status").toString()).hasSize(30); // truncado al limite de la columna
        assertThat(row.get("correlation_id").toString()).hasSize(64); // truncado al limite de la columna
    }

    @Test
    void recordSurvivesOuterRollback() { // REQUIRES_NEW: la fila sobrevive al rollback de la transaccion externa
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager); // transaccion controlada

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> { // llama al puerto con tx abierta
            externalQueryLogPort.register(new ExternalQueryLogEntry(ExternalQueryType.VESSEL_INFO, VESSEL_ID, ExternalQueryResult.OK, null));
            throw new IllegalStateException("rollback interno"); // fuerza el rollback externo
        })).isInstanceOf(IllegalStateException.class).hasMessage("rollback interno"); // el rollback llego

        Map<String, Object> row = singleRow(); // la fila quedo a pesar del rollback
        assertThat(row.get("result")).isEqualTo("OK"); // el registro propio ya estaba commiteado
    }

    @Test
    void registrationFailureDoesNotBreakQuery() { // puerto de registro roto: la consulta a M1 igual devuelve
        FleetProperties properties = new FleetProperties(); // propiedades de prueba
        properties.setBaseUrl("http://localhost:" + wireMock.port()); // apunta al WireMock
        properties.setToken("test-service-token"); // token de prueba
        RestClient client = new FleetHttpConfig().fleetRestClient(properties); // cliente real con timeouts
        ExternalQueryLogPort brokenPort = entry -> { throw new IllegalStateException("log store down"); }; // construido a mano
        HttpFleetInfoAdapter brokenInfoAdapter = new HttpFleetInfoAdapter(client, brokenPort); // adaptador con puerto roto
        HttpFleetOperationalStatusAdapter brokenStatusAdapter = new HttpFleetOperationalStatusAdapter(client, brokenPort); // adaptador con puerto roto

        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(200) // ficha valida
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Bogota"))));
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(200) // estado valido
                .withHeader("Content-Type", "application/json")
                .withBody(statusBody("AVAILABLE"))));

        FleetInfo info = brokenInfoAdapter.getVesselInfo(VESSEL_ID); // la consulta no se rompe

        assertThat(info.maxCapacity()).isEqualTo(12); // resultado intacto
        assertThat(brokenStatusAdapter.isAvailable(VESSEL_ID)).isTrue(); // resultado intacto
        assertThat(jdbcTemplate.queryForList("SELECT * FROM external_query_log")).isEmpty(); // nada se guardo
    }

    private static void warmUp() { // calienta el servidor para no chocar con el read timeout de 300 ms
        wireMock.stubFor(get(urlPathEqualTo("/warmup")).willReturn(aResponse().withStatus(200).withBody("warm"))); // stub temporal
        try {
            URLConnection connection = URI.create("http://localhost:" + wireMock.port() + "/warmup").toURL().openConnection(); // peticion cruda
            connection.setConnectTimeout(5000); // tolerante solo en el calentamiento
            connection.setReadTimeout(5000); // tolerante solo en el calentamiento
            try (InputStream in = connection.getInputStream()) {
                in.readAllBytes(); // consume la respuesta para completar la carga de clases
            }
        } catch (IOException e) {
            throw new IllegalStateException("WireMock warm up failed", e); // fallo claro si no arranca
        }
    }

    private Map<String, Object> singleRow() { // devuelve la unica fila del registro
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM external_query_log"); // todas las filas
        assertThat(rows).hasSize(1); // una sola fila por consulta
        return rows.get(0); // la unica
    }

    private static String vesselBody(String timezone) { // arma el json de la ficha
        return """
                {
                  "vessel_id": "%s",
                  "max_capacity": 12,
                  "port": { "name": "Marina", "timezone": "%s" },
                  "owner": { "owner_id": "%s" }
                }
                """.formatted(VESSEL_ID, timezone, OWNER_ID);
    }

    private static String statusBody(String status) { // arma el json del estado operativo
        return """
                { "vessel_id": "%s", "operational_status": "%s", "queried_at": "2026-10-09T12:05:00-05:00" }
                """.formatted(VESSEL_ID, status);
    }
}
