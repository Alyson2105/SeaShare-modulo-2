package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.BatchEstimateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

class HttpBatchEstimateAdapterTest { // pruebas del adaptador HTTP de estimacion en lote con WireMock

    private static final String BATCH_PATH = "/api/v1/estimates/batch"; // ruta del contrato

    private static final UUID FIRST_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // primera embarcacion fija

    private static final UUID SECOND_ID = UUID.fromString("a1c2e3f4-5678-90ab-cdef-1234567890ab"); // segunda embarcacion fija

    private WireMockServer wireMock; // servidor falso de M3

    private HttpBatchEstimateAdapter adapter; // adaptador bajo prueba

    @BeforeEach
    void setUp() { // levanta WireMock y construye el adaptador
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()); // puerto dinamico
        wireMock.start(); // arranca el servidor falso
        warmUpServer(); // la primera respuesta del servidor nuevo tarda; se calienta antes
        FinanceProperties properties = new FinanceProperties(); // propiedades de prueba
        properties.setBaseUrl("http://localhost:" + wireMock.port()); // apunta al WireMock
        RestClient client = new FinanceHttpConfig().batchFinanceRestClient(properties); // cliente real con timeouts
        adapter = new HttpBatchEstimateAdapter(client); // adaptador aislado
    }

    @AfterEach
    void tearDown() { // detiene WireMock
        wireMock.stop(); // libera el puerto
    }

    @Test
    void batch200CompleteReturnsAllEstimates() { // 200 completo: todos estimados y body con solo boat_ids
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(200) // stub con las dos embarcaciones
                .withHeader("Content-Type", "application/json")
                .withBody(batchBody(List.of(FIRST_ID, SECOND_ID), List.of()))));

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID, SECOND_ID)); // consulta real

        assertThat(result.estimates()).hasSize(2); // las dos con monto
        assertThat(result.estimates().get(FIRST_ID)).isEqualByComparingTo("150000.00"); // monto tal cual
        assertThat(result.unavailable()).isEmpty(); // nadie fallo
        wireMock.verify(postRequestedFor(urlPathEqualTo(BATCH_PATH)) // el body trae solo boat_ids
                .withRequestBody(equalToJson("{\"boat_ids\":[\"" + FIRST_ID + "\",\"" + SECOND_ID + "\"]}")));
    }

    @Test
    void batch200PartialMarksUnavailable() { // 200 con unavailable: uno estimado y otro no disponible
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(200) // stub parcial
                .withHeader("Content-Type", "application/json")
                .withBody(batchBody(List.of(FIRST_ID), List.of(SECOND_ID)))));

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID, SECOND_ID)); // consulta real

        assertThat(result.estimates()).containsOnlyKeys(FIRST_ID); // solo el primero estimado
        assertThat(result.unavailable()).containsExactly(SECOND_ID); // el segundo no disponible
    }

    @Test
    void emptyListMakesNoCallToM3() { // lista vacia: no llama a M3 y devuelve resultado vacio
        BatchEstimateResult result = adapter.getBatchEstimates(List.of()); // sin ids

        assertThat(result.estimates()).isEmpty(); // sin estimaciones
        assertThat(result.unavailable()).isEmpty(); // sin no disponibles
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(BATCH_PATH))); // cero llamadas al lote en M3
    }

    @Test
    void duplicateIdsAreDeduplicatedBeforeSend() { // ids repetidos viajan una sola vez
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(200) // stub con los dos unicos
                .withHeader("Content-Type", "application/json")
                .withBody(batchBody(List.of(FIRST_ID, SECOND_ID), List.of()))));

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID, FIRST_ID, SECOND_ID)); // id duplicado

        assertThat(result.estimates()).hasSize(2); // dos resultados unicos
        wireMock.verify(postRequestedFor(urlPathEqualTo(BATCH_PATH)) // el body sin duplicados
                .withRequestBody(equalToJson("{\"boat_ids\":[\"" + FIRST_ID + "\",\"" + SECOND_ID + "\"]}")));
    }

    @Test
    void sixtyIdsSplitIntoTwoBlocksOfFifty() { // 60 ids -> dos bloques de 50 y 10 en secuencia
        List<UUID> ids = randomIds(60); // sesenta embarcaciones
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).inScenario("blocks") // primer bloque: los primeros 50
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(batchBody(ids.subList(0, 50), List.of())))
                .willSetStateTo("SECOND"));
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).inScenario("blocks") // segundo bloque: los ultimos 10
                .whenScenarioStateIs("SECOND")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(batchBody(ids.subList(50, 60), List.of()))));

        BatchEstimateResult result = adapter.getBatchEstimates(ids); // consulta real

        assertThat(result.estimates()).hasSize(60); // todos estimados
        assertThat(result.unavailable()).isEmpty(); // nadie fallo
        wireMock.verify(2, postRequestedFor(urlPathEqualTo(BATCH_PATH))); // dos llamadas en secuencia
    }

    @Test
    void failedBlockMakesItsIdsUnavailableAndKeepsGoing() { // un bloque falla: sus ids no disponibles, el otro sigue
        List<UUID> ids = randomIds(60); // sesenta embarcaciones
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).inScenario("blocks") // primer bloque responde 500
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("SECOND"));
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).inScenario("blocks") // segundo bloque responde bien
                .whenScenarioStateIs("SECOND")
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(batchBody(ids.subList(50, 60), List.of()))));

        BatchEstimateResult result = adapter.getBatchEstimates(ids); // consulta real

        assertThat(result.unavailable()).hasSize(50); // todo el primer bloque fallo
        assertThat(result.estimates()).hasSize(10); // el segundo bloque siguio
        wireMock.verify(2, postRequestedFor(urlPathEqualTo(BATCH_PATH))); // las dos llamadas se hicieron
    }

    @Test
    void unknownIdInResponseBecomesUnavailable() { // un id que M3 no devuelve queda no disponible
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(200) // solo devuelve el primero
                .withHeader("Content-Type", "application/json")
                .withBody(batchBody(List.of(FIRST_ID), List.of()))));

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID, SECOND_ID)); // pide dos

        assertThat(result.estimates()).containsOnlyKeys(FIRST_ID); // solo el devuelto
        assertThat(result.unavailable()).containsExactly(SECOND_ID); // el ausente no disponible
    }

    @Test
    void invalidEstimatedTotalMakesVesselUnavailable() { // total ilegible en el lote deja esa embarcacion no disponible
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(200) // total roto
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimates\":[{\"boat_id\":\"" + FIRST_ID + "\",\"estimated_total\":\"abc\"}]," +
                        "\"unavailable\":[]}")));

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID)); // pide una con total roto

        assertThat(result.estimates()).isEmpty(); // nada estimado
        assertThat(result.unavailable()).containsExactly(FIRST_ID); // no disponible por monto invalido
    }

    @Test
    void serverError500MarksWholeBlockUnavailable() { // 500 -> todos los ids del bloque no disponibles
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(500))); // error del servidor

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID, SECOND_ID)); // consulta real

        assertThat(result.estimates()).isEmpty(); // nada estimado
        assertThat(result.unavailable()).containsExactlyInAnyOrder(FIRST_ID, SECOND_ID); // el bloque completo fallo
    }

    @Test
    void readTimeoutMarksBlockUnavailableWithSingleCall() { // timeout -> no disponibles y cero reintentos
        wireMock.stubFor(post(urlPathEqualTo(BATCH_PATH)).willReturn(aResponse().withStatus(200) // demora mayor al read timeout
                .withFixedDelay(1500)
                .withBody(batchBody(List.of(), List.of()))));

        BatchEstimateResult result = adapter.getBatchEstimates(List.of(FIRST_ID)); // consulta real

        assertThat(result.estimates()).isEmpty(); // nada estimado
        assertThat(result.unavailable()).containsExactly(FIRST_ID); // no disponible por timeout
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(BATCH_PATH))); // el lote nunca reintenta
    }

    private static List<UUID> randomIds(int count) { // genera ids unicos para los bloques
        List<UUID> ids = new ArrayList<>(); // lista de prueba
        for (int i = 0; i < count; i++) { // hasta el total pedido
            ids.add(UUID.randomUUID()); // id unico
        }
        return ids; // retorna la lista
    }

    private static String batchBody(List<UUID> estimated, List<UUID> unavailable) { // arma la respuesta de M3
        String estimates = estimated.stream() // entradas con monto
                .map(id -> "{\"boat_id\":\"" + id + "\",\"estimated_total\":\"150000.00\"}") // json por id
                .reduce((a, b) -> a + "," + b).orElse(""); // une con comas
        String rejects = unavailable.stream() // entradas rechazadas
                .map(id -> "{\"boat_id\":\"" + id + "\",\"reason\":\"maintenance\"}") // json por id
                .reduce((a, b) -> a + "," + b).orElse(""); // une con comas
        return "{\"estimates\":[" + estimates + "],\"unavailable\":[" + rejects + "]}"; // cuerpo completo
    }

    private void warmUpServer() { // el primer intercambio con un servidor nuevo tarda; se calienta sin el SLA
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
}
