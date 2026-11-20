package com.seashare.m2.reservas.infrastructure.adapter.out.finance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.PaymentStatus;
import com.seashare.m2.reservas.application.dto.PaymentStatusResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class HttpPaymentStatusAdapterTest { // CU-13: contrato UC06 con WireMock

    private static final UUID RESERVATION_ID = UUID.fromString("e4f81c92-7a20-4215-9c5e-8812c3f1a001");

    private static final String PATH = "/api/v1/reservations/" + RESERVATION_ID + "/payment-confirmation";

    private WireMockServer wireMock;

    private HttpPaymentStatusAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        FinanceProperties properties = new FinanceProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.port());
        adapter = new HttpPaymentStatusAdapter(new FinanceHttpConfig().paymentStatusRestClient(properties));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void approvedResponseIsParsed() { // camino feliz del contrato
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"reservation_id":"%s","status":"APPROVED","detail":null,
                         "external_reference":"pg-8841"}
                        """.formatted(RESERVATION_ID))));

        PaymentStatusResult result = adapter.getStatus(RESERVATION_ID);

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.externalReference()).isEqualTo("pg-8841");
        wireMock.verify(getRequestedFor(urlPathEqualTo(PATH)).withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void rejectedResponseCarriesTheReason() { // motivo de rechazo para auditoria
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"REJECTED\",\"detail\":\"cc_rejected\",\"external_reference\":\"pg-2\"}")));

        PaymentStatusResult result = adapter.getStatus(RESERVATION_ID);

        assertThat(result.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(result.detail()).isEqualTo("cc_rejected");
    }

    @Test
    void notFoundMeansRetryNextCycle() { // M3 aun crea la intencion de cobro
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

        assertThat(adapter.getStatus(RESERVATION_ID).status()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    void serverErrorDoesNotChangeState() { // 5xx: se reintenta en el siguiente ciclo
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        assertThat(adapter.getStatus(RESERVATION_ID).status()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    void unauthorizedIsNotTreatedAsAChargeResult() { // 401 no se interpreta
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(401)));

        assertThat(adapter.getStatus(RESERVATION_ID).status()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    void brokenJsonIsIgnored() { // nunca se asume aprobado
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ not-json")));

        assertThat(adapter.getStatus(RESERVATION_ID).status()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    void noAuthorizationHeaderIsSent() { // contrato de M3 sin credenciales
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"APPROVED\",\"external_reference\":\"pg-3\"}")));

        adapter.getStatus(RESERVATION_ID);

        wireMock.verify(getRequestedFor(urlPathEqualTo(PATH)).withoutHeader("Authorization"));
    }
}
