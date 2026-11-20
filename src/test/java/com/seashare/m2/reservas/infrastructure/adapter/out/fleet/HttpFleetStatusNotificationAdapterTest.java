package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.PendingOutboxMessage;
import com.seashare.m2.reservas.application.port.out.FleetNotificationOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class HttpFleetStatusNotificationAdapterTest { // pruebas WireMock del adaptador real de aviso de estado a M1

    private static final String PATH = "/api/v1/internal/status-events"; // ruta del contrato de M1

    private static final UUID EVENT_ID = UUID.fromString("7f3c1a9e-2b64-4d0f-9c81-5a2d6e4b1101"); // id de prueba

    private static final String PAYLOAD = // payload guardado en el outbox con los 4 campos del contrato
            """
                    {"event_id":"%s","vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","operational_status":"RESERVED","occurred_at":"2026-10-22T12:05:00-05:00"}
                    """.formatted(EVENT_ID);

    private WireMockServer wireMock; // servidor falso de M1

    private HttpFleetStatusNotificationAdapter adapter; // adaptador bajo prueba

    @BeforeEach
    void setUp() { // levanta WireMock y el adaptador con timeouts de prueba
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        FleetProperties properties = new FleetProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.port());
        properties.setConnectTimeoutMs(1000);
        adapter = new HttpFleetStatusNotificationAdapter(properties);
    }

    @AfterEach
    void tearDown() { // libera el puerto
        wireMock.stop();
    }

    private PendingOutboxMessage message() { // mensaje pendiente minimo para el puerto
        return new PendingOutboxMessage(UUID.randomUUID(), EVENT_ID, "M1_HTTP", null, null, PAYLOAD, Instant.parse("2026-10-22T17:05:00Z"), 0);
    }

    private void stub200(String outcomeBody) { // stub de 200 con el body indicado
        wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(outcomeBody)));
    }

    @Test
    void deliveredOutcomesMapToDelivered() { // APPLIED, DUPLICATE, NO_CHANGE y STALE = entregado
        for (String outcome : new String[]{"APPLIED", "DUPLICATE", "NO_CHANGE", "STALE"}) {
            stub200("{\"event_id\":\"" + EVENT_ID + "\",\"outcome\":\"" + outcome + "\"}");

            assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.DELIVERED); // fila entregada
            wireMock.resetAll(); // limpia el stub entre outcomes
        }
    }

    @Test
    void rejectionOutcomesMapToPermanentFailure() { // UNKNOWN_VESSEL e INVALID_TRANSITION = fallo definitivo
        for (String outcome : new String[]{"UNKNOWN_VESSEL", "INVALID_TRANSITION"}) {
            stub200("{\"event_id\":\"" + EVENT_ID + "\",\"outcome\":\"" + outcome + "\"}");

            assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.PERMANENT_FAILURE); // dead directo
            wireMock.resetAll(); // limpia el stub entre outcomes
        }
    }

    @Test
    void badRequestMapsToPermanentFailure() { // 400 = aviso invalido, sin reintento
        wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(400)));

        assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.PERMANENT_FAILURE); // definitivo
    }

    @Test
    void serverErrorMapsToRetryableFailure() { // 5xx = transitorio
        wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.RETRYABLE_FAILURE); // reintento
    }

    @Test
    void missingOrUnknownOutcomeMapsToRetryableFailure() { // outcome ausente o fuera de catalogo = reintento
        stub200("{\"event_id\":\"" + EVENT_ID + "\"}"); // sin outcome
        assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.RETRYABLE_FAILURE); // conservador
        wireMock.resetAll(); // limpia para el segundo caso

        stub200("{\"event_id\":\"" + EVENT_ID + "\",\"outcome\":\"SOMETHING_ELSE\"}"); // outcome desconocido
        assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.RETRYABLE_FAILURE); // no clasificable
    }

    @Test
    void connectionErrorMapsToRetryableFailure() { // servidor caido = reintento
        wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(200).withBody("{}")));
        wireMock.stop(); // corta la conexion antes de llamar

        assertThat(adapter.notifyStatus(message())).isEqualTo(FleetNotificationOutcome.RETRYABLE_FAILURE); // inalcanzable
    }

    @Test
    void sendsPayloadAsJsonBody() { // el payload del outbox viaja tal cual en el cuerpo
        stub200("{\"event_id\":\"" + EVENT_ID + "\",\"outcome\":\"APPLIED\"}");

        adapter.notifyStatus(message());

        wireMock.verify(postRequestedFor(urlPathEqualTo(PATH)).withRequestBody(containing("\"operational_status\":\"RESERVED\""))); // 4 campos presentes
        wireMock.verify(postRequestedFor(urlPathEqualTo(PATH)).withRequestBody(containing("\"event_id\":\"" + EVENT_ID + "\""))); // event_id en el cuerpo
    }
}
