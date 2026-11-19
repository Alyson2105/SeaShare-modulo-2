package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.port.out.ExternalQueryLogPort;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpFleetClientTest { // pruebas de exito de los adaptadores HTTP reales de flota con WireMock

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // id fijo de prueba
    private static final UUID OWNER_ID = UUID.fromString("a1c2e3f4-5678-90ab-cdef-1234567890ab"); // propietario fijo
    private static final String VESSEL_PATH = "/api/v1/vessels/" + VESSEL_ID; // ruta de la ficha
    private static final String STATUS_PATH = VESSEL_PATH + "/operational-status"; // ruta del estado operativo
    private static final LocalDate START = LocalDate.of(2026, 12, 20); // inicio del rango de prueba
    private static final LocalDate END = LocalDate.of(2026, 12, 22); // fin del rango de prueba
    private static final ZoneId ZONE = ZoneId.of("America/Bogota"); // zona del puerto de prueba
    private static final Instant NOW = Instant.parse("2026-12-01T00:00:00Z"); // reloj fijo de prueba

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
        RestClient client = new FleetHttpConfig().fleetRestClient(properties); // cliente real con timeouts
        ExternalQueryLogPort noopLog = entry -> { }; // el registro no se prueba aqui
        infoAdapter = new HttpFleetInfoAdapter(client, noopLog); // adaptador de ficha
        statusAdapter = new HttpFleetOperationalStatusAdapter(client, noopLog, () -> NOW); // adaptador de estado
    }

    @AfterEach
    void tearDown() { // detiene WireMock
        wireMock.stop(); // libera el puerto
    }

    @Test
    void mapsVesselInfoFromSuccessfulResponse() { // 200 con datos completos
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse() // stub de la ficha
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Bogota")))); // cuerpo con zona horaria valida

        FleetInfo info = infoAdapter.getVesselInfo(VESSEL_ID); // consulta real contra WireMock

        assertThat(info.ownerId()).isEqualTo(OWNER_ID); // owner_id mapeado
        assertThat(info.maxCapacity()).isEqualTo(12); // max_capacity mapeado
        assertThat(info.harborTimezone()).isEqualTo("America/Bogota"); // port.timezone mapeado
    }

    @Test
    void internalServiceTokenHeaderIsNotSent() { // contrato de M1 sin autenticacion: el header no viaja
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)).willReturn(aResponse() // stub de la ficha
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(vesselBody("America/Bogota"))));

        infoAdapter.getVesselInfo(VESSEL_ID); // ejecuta la consulta

        wireMock.verify(getRequestedFor(urlPathEqualTo(VESSEL_PATH)) // verifica la llamada
                .withoutHeader("X-Internal-Service-Token")); // sin el header de token
    }

    @Test
    void retriesOnceAfter503AndSucceeds() { // 503 seguido de 200 en el reintento
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)) // primer intento: 503
                .inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second"));
        wireMock.stubFor(get(urlPathEqualTo(VESSEL_PATH)) // segundo intento: 200
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(vesselBody("America/Bogota"))));

        FleetInfo info = infoAdapter.getVesselInfo(VESSEL_ID); // el reintento devuelve datos

        assertThat(info.maxCapacity()).isEqualTo(12); // confirmacion del reintento exitoso
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(VESSEL_PATH))); // exactamente dos llamadas
    }

    @Test
    void availableReservedAndNavigationAreAvailable() { // estados sin mantenimiento habilitan el rango
        for (String status : List.of("AVAILABLE", "RESERVED", "NAVIGATION", "IN_NAVIGATION")) {
            wireMock.resetAll(); // limpia stubs y llamadas entre iteraciones
            wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse() // stub del estado actual
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(statusBody(status))));

            assertThat(statusAdapter.isAvailable(VESSEL_ID, START, END, ZONE)).as(status).isTrue();
        }
    }

    @Test
    void maintenanceWithoutDatesIsNotAvailable() { // mantenimiento sin ventana: fail-safe
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse() // sin fechas de mantenimiento
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(statusBody("MAINTENANCE_CLEANING"))));

        assertThat(statusAdapter.isAvailable(VESSEL_ID, START, END, ZONE)).isFalse();
    }

    @Test
    void maintenanceOutsideRangeIsAvailable() { // ventana que no toca ningun dia de la reserva
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse() // mantenimiento posterior
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(maintenanceBody("2026-12-23T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))));

        assertThat(statusAdapter.isAvailable(VESSEL_ID, START, END, ZONE)).isTrue();
    }

    @Test
    void maintenanceCrossingRangeIsNotAvailable() { // ventana que toca el rango inclusivo
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse() // mantenimiento solapado
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(maintenanceBody("2026-12-21T08:00:00-05:00", "2026-12-27T18:00:00-05:00"))));

        assertThat(statusAdapter.isAvailable(VESSEL_ID, START, END, ZONE)).isFalse();
    }

    @Test
    void expiredMaintenanceIsAvailable() { // maintenance_until vencido: liberacion perezosa (D-07)
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse() // ventana ya vencida
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(maintenanceBody("2026-11-20T08:00:00-05:00", "2026-11-27T18:00:00-05:00"))));

        assertThat(statusAdapter.isAvailable(VESSEL_ID, START, END, ZONE)).isTrue();
    }

    @Test
    void unknownStatusFailsSafe() { // estado desconocido: FLEET_SERVICE_UNAVAILABLE
        wireMock.stubFor(get(urlPathEqualTo(STATUS_PATH)).willReturn(aResponse() // estado fuera del contrato
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(statusBody("WEIRD_STATUS"))));

        assertThatThrownBy(() -> statusAdapter.isAvailable(VESSEL_ID, START, END, ZONE))
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.FLEET_SERVICE_UNAVAILABLE.getCode()));
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

    private static String statusBody(String status) { // arma el json del estado operativo
        return """
                { "vessel_id": "%s", "operational_status": "%s", "queried_at": "2026-10-09T12:05:00-05:00" }
                """.formatted(VESSEL_ID, status);
    }

    private static String maintenanceBody(String start, String until) { // arma el json con ventana de mantenimiento
        return """
                {
                  "vessel_id": "%s",
                  "operational_status": "MAINTENANCE",
                  "queried_at": "2026-10-09T12:05:00-05:00",
                  "maintenance_start": "%s",
                  "maintenance_until": "%s"
                }
                """.formatted(VESSEL_ID, start, until);
    }
}
