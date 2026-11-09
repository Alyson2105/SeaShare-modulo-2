package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.IndividualEstimate;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpIndividualEstimateAdapterTest { // pruebas del adaptador HTTP de estimacion individual con WireMock

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // id fijo de prueba

    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1); // fecha de inicio fija

    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 5); // fecha de fin fija

    private static final int PASSENGERS = 4; // pasajeros fijos

    private static final String INDIVIDUAL_PATH = "/api/v1/estimates/individual"; // ruta del contrato

    private WireMockServer wireMock; // servidor falso de M3

    private HttpIndividualEstimateAdapter adapter; // adaptador bajo prueba

    @BeforeEach
    void setUp() { // levanta WireMock y construye el adaptador
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()); // puerto dinamico
        wireMock.start(); // arranca el servidor falso
        warmUpServer(); // la primera respuesta del servidor nuevo tarda; se calienta antes
        FinanceProperties properties = new FinanceProperties(); // propiedades de prueba
        properties.setBaseUrl("http://localhost:" + wireMock.port()); // apunta al WireMock
        RestClient client = new FinanceHttpConfig().individualFinanceRestClient(properties); // cliente real con timeouts
        adapter = new HttpIndividualEstimateAdapter(client); // adaptador aislado
    }

    @AfterEach
    void tearDown() { // detiene WireMock
        wireMock.stop(); // libera el puerto
    }

    @Test
    void successful200WithWarning() { // 200 con warning se conserva literal
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // stub con warning
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimated_total\":\"150000.00\",\"warning\":\"Credit risk review pending\"}")));

        IndividualEstimate estimate = adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS); // consulta real

        assertThat(estimate.estimatedTotal()).isEqualByComparingTo("150000.00"); // monto tal cual, sin calcular
        assertThat(estimate.warning()).isEqualTo("Credit risk review pending"); // warning literal
        wireMock.verify(postRequestedFor(urlPathEqualTo(INDIVIDUAL_PATH)) // sin header de autorizacion
                .withoutHeader("Authorization"));
    }

    @Test
    void successful200WithoutWarning() { // 200 sin warning deja warning en null y no es error
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // stub sin warning
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimated_total\":\"99000.50\",\"extra_field\":\"ignored\"}"))); // campo desconocido se ignora

        IndividualEstimate estimate = adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS); // consulta real

        assertThat(estimate.estimatedTotal()).isEqualByComparingTo("99000.50"); // monto tal cual
        assertThat(estimate.warning()).isNull(); // sin warning
    }

    @Test
    void emptyTotalThrowsQuotationUnavailable() { // estimated_total vacio -> QUOTATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // stub con total vacio
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimated_total\":\"\"}")));

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void zeroTotalThrowsQuotationUnavailable() { // estimated_total cero -> QUOTATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // stub con total cero
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimated_total\":\"0\"}")));

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void negativeTotalThrowsQuotationUnavailable() { // estimated_total negativo -> QUOTATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // stub con total negativo
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimated_total\":\"-5000.00\"}")));

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void unparseableTotalThrowsQuotationUnavailable() { // estimated_total ilegible -> QUOTATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // stub con total roto
                .withHeader("Content-Type", "application/json")
                .withBody("{\"estimated_total\":\"abc\"}")));

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void badRequest400ThrowsInvalidParameters() { // 400 -> INVALID_PARAMETERS sin reintento
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(400))); // parametros rechazados

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(INDIVIDUAL_PATH))); // un solo intento
    }

    @Test
    void unauthorized401ThrowsInternalError() { // 401 -> INTERNAL_ERROR
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(401))); // acceso no autenticado

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado
    }

    @Test
    void forbidden403ThrowsInternalError() { // 403 -> INTERNAL_ERROR
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(403))); // acceso denegado

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado
    }

    @Test
    void unprocessable422ThrowsQuotationUnavailable() { // 422 -> QUOTATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(422))); // entidad no procesable

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(INDIVIDUAL_PATH))); // ningun 4xx se reintenta
    }

    @Test
    void serverError500ThrowsQuotationUnavailable() { // 500 -> QUOTATION_UNAVAILABLE sin reintento
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(500))); // error del servidor

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(INDIVIDUAL_PATH))); // un solo intento
    }

    @Test
    void serviceUnavailable503ThrowsQuotationUnavailableWithoutRetry() { // 503 -> QUOTATION_UNAVAILABLE sin reintento
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(503))); // servicio caido

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(INDIVIDUAL_PATH))); // el reintento es solo por ConnectException
    }

    @Test
    void readTimeoutThrowsQuotationUnavailableWithoutRetry() { // read timeout -> QUOTATION_UNAVAILABLE con un solo intento
        wireMock.stubFor(post(urlPathEqualTo(INDIVIDUAL_PATH)).willReturn(aResponse().withStatus(200) // demora mayor al read timeout
                .withFixedDelay(1500)
                .withBody("{\"estimated_total\":\"100.00\"}")));

        assertThatThrownBy(() -> adapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // corta por timeout
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(INDIVIDUAL_PATH))); // SocketTimeoutException nunca reintenta
    }

    @Test
    void connectionRefusedRetriesOnceThenFails() { // conexion rechazada: reintenta una vez y termina en QUOTATION_UNAVAILABLE
        FinanceProperties closed = new FinanceProperties(); // propiedades hacia un puerto muerto
        closed.setBaseUrl("http://127.0.0.1:1"); // puerto 1 siempre cerrado en local
        HttpIndividualEstimateAdapter closedAdapter = new HttpIndividualEstimateAdapter( // adaptador aislado
                new FinanceHttpConfig().individualFinanceRestClient(closed));

        assertThatThrownBy(() -> closedAdapter.getIndividualEstimate(VESSEL_ID, START_DATE, END_DATE, PASSENGERS)) // dos intentos fallidos
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void retryDecisionOnlyForConnectException() { // el reintento depende unicamente de ConnectException
        assertThat(HttpIndividualEstimateAdapter.isRetryableConnectionFailure( // conexion rechazada envuelta
                new ResourceAccessException("failed", new ConnectException("Connection refused")))).isTrue(); // si reintenta
        assertThat(HttpIndividualEstimateAdapter.isRetryableConnectionFailure( // read timeout
                new ResourceAccessException("failed", new SocketTimeoutException("Read timed out")))).isFalse(); // no reintenta
        assertThat(HttpIndividualEstimateAdapter.isRetryableConnectionFailure( // connect timeout
                new ResourceAccessException("failed", new SocketTimeoutException("connect timed out")))).isFalse(); // no reintenta
        assertThat(HttpIndividualEstimateAdapter.isRetryableConnectionFailure(new IOException("boom"))).isFalse(); // otro IOException no reintenta
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
