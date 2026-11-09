package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FleetUnavailableTest { // pruebas de cortes seguros y errores de M1 con WireMock

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // id fijo de prueba
    private static final UUID OWNER_ID = UUID.fromString("a1c2e3f4-5678-90ab-cdef-1234567890ab"); // propietario fijo
    private static final String TOKEN = "test-service-token"; // token usado en las pruebas
    private static final String VESSEL_PATH = "/api/v1/vessels/" + VESSEL_ID; // ruta de la ficha
    private static final String STATUS_PATH = VESSEL_PATH + "/operational-status"; // ruta del estado operativo

    private WireMockServer wireMock; // servidor falso de M1
    private HttpFleetInfoAdapter infoAdapter; // adaptador de ficha bajo prueba
    private HttpFleetOperationalStatusAdapter statusAdapter; // adaptador de estado bajo prueba

    @BeforeEach
    void setUp() { // levanta WireMock y construye los adaptadores
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()); // puerto dinamico
        wireMock.start(); // arranca el servidor falso
        warmUpServer(); // la primera respuesta del servidor nuevo tarda mas de 300 ms
        FleetProperties properties = new FleetProperties(); // propiedades de prueba
        properties.setBaseUrl("http://localhost:" + wireMock.port()); // apunta al WireMock
        properties.setToken(TOKEN); // token de prueba
        RestClient client = new FleetHttpConfig().fleetRestClient(properties); // cliente real con timeouts
        infoAdapter = new HttpFleetInfoAdapter(client); // adaptador de ficha
        statusAdapter = new HttpFleetOperationalStatusAdapter(client); // adaptador de estado
    }

    @AfterEach
    void tearDown() { // detiene WireMock
        wireMock.stop(); // libera el puerto
    }

    @Test
    void vesselNotFoundOn404WithoutRetry() { // 404 -> VESSEL_NOT_FOUND con una sola llamada
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(404))); // sin cuerpo

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corta en la primera llamada
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode())); // codigo esperado

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // cero reintentos ante 4xx
    }

    @Test
    void invalidUuidOn400WithoutRetry() { // 400 -> INVALID_UUID con una sola llamada
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(400))); // sin cuerpo

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corta en la primera llamada
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_UUID.getCode())); // codigo esperado

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // cero reintentos ante 4xx
    }

    @Test
    void rejectedTokenOn401MapsToInternalError() { // 401 -> INTERNAL_ERROR
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(401))); // sin cuerpo

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corta sin reintento
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // una sola llamada
    }

    @Test
    void forbiddenTokenOn403MapsToInternalError() { // 403 -> INTERNAL_ERROR
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(403))); // sin cuerpo

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // corta sin reintento
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // una sola llamada
    }

    @Test
    void double503FailsSafeAfterSingleRetry() { // 503 dos veces -> FLEET_SERVICE_UNAVAILABLE
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse().withStatus(503))); // siempre 503

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // agota el reintento
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(2, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // un reintento y corte
    }

    @Test
    void timeoutRetriesOnceThenFailsSafe() { // demora mayor al read timeout -> FLEET_SERVICE_UNAVAILABLE
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse() // respuesta tardia
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Bogota"))
                .withFixedDelay(800))); // 800 ms superan el read timeout de 300 ms

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // ambos intentos expiran
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(2, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // un reintento y corte
    }

    @Test
    void connectionRefusedFailsSafe() { // puerto cerrado -> IOException -> FLEET_SERVICE_UNAVAILABLE
        FleetProperties closed = new FleetProperties(); // propiedades hacia un puerto muerto
        closed.setBaseUrl("http://127.0.0.1:1"); // puerto 1 siempre cerrado en local
        closed.setToken(TOKEN); // token de prueba
        HttpFleetInfoAdapter closedAdapter = new HttpFleetInfoAdapter(new FleetHttpConfig().fleetRestClient(closed)); // adaptador aislado

        assertThatThrownBy(() -> closedAdapter.getVesselInfo(VESSEL_ID)) // conexion rechazada dos veces
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void missingHarborTimezoneFailsSafe() { // 200 sin port.timezone -> HARBOR_TIMEZONE_UNAVAILABLE
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse() // cuerpo sin puerto
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        { "vessel_id": "%s", "max_capacity": 12, "owner": { "owner_id": "%s" } }
                        """.formatted(VESSEL_ID, OWNER_ID))));

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // sin zona horaria no hay plazos
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void invalidHarborTimezoneFailsSafe() { // 200 con timezone invalida -> HARBOR_TIMEZONE_UNAVAILABLE
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse() // cuerpo con zona mala
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Nope"))));

        assertThatThrownBy(() -> infoAdapter.getVesselInfo(VESSEL_ID)) // ZoneId.of la rechaza
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.HARBOR_TIMEZONE_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void operationalStatusNotFoundOn404() { // el adaptador de CU-10 comparte el mapeo de errores
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(404))); // sin cuerpo

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID)) // corta en la primera llamada
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode())); // codigo esperado

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(STATUS_PATH))); // cero reintentos ante 4xx
    }

    private void warmUpServer() { // el primer intercambio con un servidor nuevo tarda mas de 300 ms; se calienta sin el SLA
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
}
