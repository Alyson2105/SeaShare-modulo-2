package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.CalculatedValue;
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
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLConnection;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpCalculatedValueAdapterTest { // pruebas del adaptador HTTP del valor calculado con WireMock

    private static final UUID RESERVATION_ID = UUID.fromString("e4f81c92-7a20-4215-9c5e-8812c3f1a001"); // reserva fija

    private static final String CALCULATED_PATH = "/api/v1/reservations/" + RESERVATION_ID + "/calculated-value"; // ruta del contrato

    private static final String FULL_BODY = // respuesta completa con un campo de mas que se ignora
            """
            {
              "reservation_id": "%s",
              "rental_amount": "9600000.00",
              "insurance_amount": "120000.00",
              "guarantee_deposit_amount": "320000.00",
              "total_amount": "10040000.00",
              "currency": "COP"
            }
            """.formatted(RESERVATION_ID);

    private WireMockServer wireMock; // servidor falso de M3

    private FinanceProperties properties; // propiedades de prueba

    private HttpCalculatedValueAdapter adapter; // adaptador bajo prueba

    @BeforeEach
    void setUp() { // levanta WireMock y construye el adaptador
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()); // puerto dinamico
        wireMock.start(); // arranca el servidor falso
        warmUpServer(); // la primera respuesta del servidor nuevo tarda; se calienta antes
        properties = new FinanceProperties(); // propiedades de prueba
        properties.setBaseUrl("http://localhost:" + wireMock.port()); // apunta al WireMock
        adapter = buildAdapter(properties); // adaptador sin esperas
    }

    @AfterEach
    void tearDown() { // detiene WireMock
        wireMock.stop(); // libera el puerto
    }

    @Test
    void correct200ReturnsBreakdown() { // 200 completo devuelve los cuatro montos tal cual
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // stub completo
                .withHeader("Content-Type", "application/json")
                .withBody(FULL_BODY)));

        CalculatedValue value = adapter.getCalculatedValue(RESERVATION_ID); // consulta real

        assertThat(value.rentalAmount()).isEqualByComparingTo("9600000.00"); // alquiler tal cual
        assertThat(value.insuranceAmount()).isEqualByComparingTo("120000.00"); // seguro tal cual
        assertThat(value.guaranteeDepositAmount()).isEqualByComparingTo("320000.00"); // deposito tal cual
        assertThat(value.totalAmount()).isEqualByComparingTo("10040000.00"); // total tal cual, sin recomponer
        wireMock.verify(postRequestedFor(urlPathEqualTo(CALCULATED_PATH)) // header Accept del contrato
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void noAuthorizationHeaderIsSent() { // nunca viaja Authorization hacia M3
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // stub valido
                .withHeader("Content-Type", "application/json")
                .withBody(FULL_BODY)));

        adapter.getCalculatedValue(RESERVATION_ID); // consulta real

        wireMock.verify(postRequestedFor(urlPathEqualTo(CALCULATED_PATH)) // sin Authorization
                .withoutHeader("Authorization"));
    }

    @Test
    void missingFieldThrowsCalculationUnavailable() { // rental_amount ausente -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // sin alquiler
                .withHeader("Content-Type", "application/json")
                .withBody("{\"insurance_amount\":\"1.00\",\"guarantee_deposit_amount\":\"1.00\",\"total_amount\":\"2.00\"}")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void nonNumericAmountThrowsCalculationUnavailable() { // rental_amount no numerico -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // monto roto
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rental_amount\":\"abc\",\"insurance_amount\":\"1.00\",\"guarantee_deposit_amount\":\"1.00\",\"total_amount\":\"2.00\"}")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void zeroTotalThrowsCalculationUnavailable() { // total en cero -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // total cero
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rental_amount\":\"1.00\",\"insurance_amount\":\"1.00\",\"guarantee_deposit_amount\":\"1.00\",\"total_amount\":\"0\"}")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void negativeTotalThrowsCalculationUnavailable() { // total negativo -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // total negativo
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rental_amount\":\"1.00\",\"insurance_amount\":\"1.00\",\"guarantee_deposit_amount\":\"1.00\",\"total_amount\":\"-5.00\"}")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void negativeRentalThrowsCalculationUnavailable() { // alquiler negativo -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // alquiler negativo
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rental_amount\":\"-1.00\",\"insurance_amount\":\"1.00\",\"guarantee_deposit_amount\":\"1.00\",\"total_amount\":\"1.00\"}")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void badRequest400ThrowsInternalError() { // 400 -> INTERNAL_ERROR sin reintento
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(400))); // peticion invalida

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // un solo intento
    }

    @Test
    void unauthorized401ThrowsInternalError() { // 401 -> INTERNAL_ERROR
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(401))); // no autenticado

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado
    }

    @Test
    void forbidden403ThrowsInternalError() { // 403 -> INTERNAL_ERROR
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(403))); // denegado

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode())); // codigo esperado
    }

    @Test
    void notFoundFailsImmediatelyWithoutRetry() { // 404: no se espera en el hilo; el cliente reintenta despues
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(404))); // aun sin informar

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte inmediato
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // una sola llamada, sin reintentos
    }

    @Test
    void persistentNotFoundThrowsCalculationUnavailable() { // 404 persistente: una sola llamada
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(404))); // siempre sin informar

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // sin bucle de reintentos
    }

    @Test
    void unprocessable422ThrowsCalculationUnavailable() { // 422 -> CALCULATION_UNAVAILABLE sin reintento
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(422))); // informacion incompleta

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // un solo intento
    }

    @Test
    void serviceUnavailable503ThrowsCalculationUnavailable() { // 503 -> CALCULATION_UNAVAILABLE sin reintento
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(503))); // servicio caido

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // el reintento por status es solo del 404
    }

    @Test
    void serverError500ThrowsCalculationUnavailable() { // 500 -> CALCULATION_UNAVAILABLE sin reintento
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(500))); // error del servidor

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // un solo intento
    }

    @Test
    void readTimeoutThrowsCalculationUnavailableWithoutRetry() { // read timeout -> sin reintento
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // demora mayor al read timeout
                .withFixedDelay(1500)
                .withBody(FULL_BODY)));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corta por timeout
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(CALCULATED_PATH))); // SocketTimeoutException nunca reintenta
    }

    @Test
    void connectionRefusedRetriesOnceThenFails() { // conexion rechazada: un reintento y luego CALCULATION_UNAVAILABLE
        FinanceProperties closed = new FinanceProperties(); // propiedades hacia un puerto muerto
        closed.setBaseUrl("http://127.0.0.1:1"); // puerto 1 siempre cerrado en local
        HttpCalculatedValueAdapter closedAdapter = buildAdapter(closed); // adaptador aislado

        assertThatThrownBy(() -> closedAdapter.getCalculatedValue(RESERVATION_ID)) // dos intentos fallidos
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void retryDecisionOnlyForConnectException() { // el reintento de socket depende unicamente de ConnectException
        assertThat(HttpCalculatedValueAdapter.isRetryableConnectionFailure( // conexion rechazada envuelta
                new ResourceAccessException("failed", new ConnectException("Connection refused")))).isTrue(); // si reintenta
        assertThat(HttpCalculatedValueAdapter.isRetryableConnectionFailure( // read timeout
                new ResourceAccessException("failed", new SocketTimeoutException("Read timed out")))).isFalse(); // no reintenta
        assertThat(HttpCalculatedValueAdapter.isRetryableConnectionFailure( // connect timeout
                new ResourceAccessException("failed", new SocketTimeoutException("connect timed out")))).isFalse(); // no reintenta
        assertThat(HttpCalculatedValueAdapter.isRetryableConnectionFailure(new IOException("boom"))).isFalse(); // otro IOException no reintenta
    }

    @Test
    void emptyBodyThrowsCalculationUnavailable() { // 200 sin cuerpo -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200))); // cuerpo vacio

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void brokenJsonThrowsCalculationUnavailable() { // 200 ilegible -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // json roto
                .withHeader("Content-Type", "application/json")
                .withBody("{ not-json")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void zeroRentalThrowsCalculationUnavailable() { // alquiler en cero -> CALCULATION_UNAVAILABLE
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // alquiler cero
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rental_amount\":\"0\",\"insurance_amount\":\"1.00\",\"guarantee_deposit_amount\":\"1.00\",\"total_amount\":\"2.00\"}")));

        assertThatThrownBy(() -> adapter.getCalculatedValue(RESERVATION_ID)) // corte seguro
                .isInstanceOfSatisfying(ReservationRuleException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ErrorCode.CALCULATION_UNAVAILABLE.getCode())); // codigo esperado
    }

    @Test
    void zeroInsuranceAndDepositAreAccepted() { // seguro y deposito en cero son validos
        wireMock.stubFor(post(urlPathEqualTo(CALCULATED_PATH)).willReturn(aResponse().withStatus(200) // sin seguro ni deposito
                .withHeader("Content-Type", "application/json")
                .withBody("{\"rental_amount\":\"1.00\",\"insurance_amount\":\"0\",\"guarantee_deposit_amount\":\"0.00\",\"total_amount\":\"1.00\"}")));

        CalculatedValue value = adapter.getCalculatedValue(RESERVATION_ID); // consulta real

        assertThat(value.insuranceAmount()).isEqualByComparingTo("0"); // seguro cero tal cual
        assertThat(value.guaranteeDepositAmount()).isEqualByComparingTo("0"); // deposito cero tal cual
        assertThat(value.totalAmount()).isEqualByComparingTo("1.00"); // total tal cual
    }

    private static HttpCalculatedValueAdapter buildAdapter(FinanceProperties props) { // construye el adaptador aislado
        RestClient client = new FinanceHttpConfig().calculatedValueRestClient(props); // cliente real con timeouts
        return new HttpCalculatedValueAdapter(client); // adaptador aislado
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
