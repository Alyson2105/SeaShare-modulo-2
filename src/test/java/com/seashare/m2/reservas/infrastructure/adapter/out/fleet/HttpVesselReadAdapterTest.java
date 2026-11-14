package com.seashare.m2.reservas.infrastructure.adapter.out.fleet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpVesselReadAdapterTest { // pruebas WireMock del adaptador de ficha y resumen de flota

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // id de prueba

    private static final UUID OTHER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555"); // segundo id

    private static final String PATH = "/api/v1/vessels/" + VESSEL_ID; // ruta individual

    private WireMockServer wireMock; // servidor falso de M1

    private HttpVesselReadAdapter adapter; // adaptador bajo prueba

    @BeforeEach
    void setUp() { // levanta WireMock con timeouts holgados
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        FleetProperties properties = new FleetProperties();
        properties.setBaseUrl("http://localhost:" + wireMock.port());
        properties.setConnectTimeoutMs(1000);
        properties.setReadTimeoutMs(2000); // evita flakiness del primer request
        RestClient client = new FleetHttpConfig().fleetRestClient(properties);
        adapter = new HttpVesselReadAdapter(client);
    }

    @AfterEach
    void tearDown() { // libera el puerto
        wireMock.stop();
    }

    private String body() { // respuesta completa del contrato CU-09
        return """
                {
                  "vessel_id": "d3b07384-d113-49cd-a5d6-812e9bcfc101",
                  "name": "Yate Tayrona",
                  "registration_number": "CP-04-2021-0892",
                  "type": "YACHT",
                  "max_capacity": 12,
                  "length_feet": 48.5,
                  "cabin_count": 3,
                  "captain_included": true,
                  "port": {
                    "name": "Marina Santa Marta",
                    "latitude": 11.2443,
                    "longitude": -74.2125,
                    "timezone": "America/Bogota"
                  },
                  "amenities": ["GPS", "Chalecos"],
                  "owner": {
                    "owner_id": "a1c2e3f4-5678-90ab-cdef-1234567890ab",
                    "validated_name": "Inversiones Nauticas",
                    "verified": true
                  }
                }
                """;
    }

    @Test
    void mapsFullDetail() { // ficha completa con todos los campos del contrato
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body())));

        VesselDetail detail = adapter.getDetail(VESSEL_ID);

        assertThat(detail.name()).isEqualTo("Yate Tayrona");
        assertThat(detail.registrationNumber()).isEqualTo("CP-04-2021-0892");
        assertThat(detail.type()).isEqualTo("YACHT");
        assertThat(detail.maxCapacity()).isEqualTo(12);
        assertThat(detail.lengthFeet()).isEqualTo(48.5);
        assertThat(detail.cabinCount()).isEqualTo(3);
        assertThat(detail.captainIncluded()).isTrue();
        assertThat(detail.portName()).isEqualTo("Marina Santa Marta");
        assertThat(detail.portLatitude()).isEqualTo(11.2443);
        assertThat(detail.portLongitude()).isEqualTo(-74.2125);
        assertThat(detail.timezone()).isEqualTo("America/Bogota");
        assertThat(detail.amenities()).containsExactly("GPS", "Chalecos");
        assertThat(detail.ownerName()).isEqualTo("Inversiones Nauticas");
        assertThat(detail.ownerVerified()).isTrue();
    }

    @Test
    void notFoundMapsToVesselNotFound() { // 404 conserva el codigo de CU-09
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> adapter.getDetail(VESSEL_ID))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode()));
    }

    @Test
    void summariesSkipFailedElements() { // fail-safe por elemento en el resumen de listas
        wireMock.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body())));
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/vessels/" + OTHER_ID)).willReturn(aResponse().withStatus(500)));

        Map<UUID, VesselSummary> summaries = adapter.getSummaries(List.of(VESSEL_ID, OTHER_ID));

        assertThat(summaries).containsOnlyKeys(VESSEL_ID);
        assertThat(summaries.get(VESSEL_ID).name()).isEqualTo("Yate Tayrona");
        assertThat(summaries.get(VESSEL_ID).ownerName()).isEqualTo("Inversiones Nauticas");
    }
}
