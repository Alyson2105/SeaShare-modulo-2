package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
class VesselDetailControllerTest { // pruebas MockMvc de CU-19 con los adaptadores fake por defecto

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // embarcacion de prueba

    private static final String RENTER = UUID.randomUUID().toString(); // arrendatario de prueba

    @Autowired
    private MockMvc mockMvc; // cliente MockMvc

    @Test
    void vesselDetailReturnsFleetDataWithoutCache() throws Exception { // Endpoint 1 con no-store
        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID)
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.vessel_id").value(VESSEL_ID.toString()))
                .andExpect(jsonPath("$.name").value(org.hamcrest.Matchers.startsWith("Embarcacion demo")))
                .andExpect(jsonPath("$.registration_number").value(org.hamcrest.Matchers.startsWith("REG-DEMO-")))
                .andExpect(jsonPath("$.navigation_type").value("CATAMARAN"))
                .andExpect(jsonPath("$.max_capacity").value(12))
                .andExpect(jsonPath("$.length_feet").value(48.5))
                .andExpect(jsonPath("$.cabin_count").value(3))
                .andExpect(jsonPath("$.captain_included").value(true))
                .andExpect(jsonPath("$.port.name").value("Marina demo"))
                .andExpect(jsonPath("$.port.timezone").value("America/Bogota"))
                .andExpect(jsonPath("$.amenities[0]").value("GPS Nautico"))
                .andExpect(jsonPath("$.owner.validated_name").value("Propietario demo"))
                .andExpect(jsonPath("$.owner.verified").value(true));
    }

    @Test
    void quotationReturnsLiteralValuesAndInclusiveDays() throws Exception { // Endpoint 2 sin calculos monetarios
        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID + "/quotations")
                        .param("start_date", "2027-04-10")
                        .param("end_date", "2027-04-12")
                        .param("passengers", "4")
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.vessel_id").value(VESSEL_ID.toString()))
                .andExpect(jsonPath("$.start_date").value("2027-04-10"))
                .andExpect(jsonPath("$.end_date").value("2027-04-12"))
                .andExpect(jsonPath("$.total_days").value(3))
                .andExpect(jsonPath("$.passengers").value(4))
                .andExpect(jsonPath("$.total_amount").isNumber())
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.warning").value("fake estimate warning"))
                .andExpect(jsonPath("$.quotation_id").value(nullValue()))
                .andExpect(jsonPath("$.quotation_expires_at").value(nullValue()));
    }

    @Test
    void capacityExceededIs409() throws Exception { // revalidacion de capacidad contra M1
        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID + "/quotations")
                        .param("start_date", "2027-04-10")
                        .param("end_date", "2027-04-12")
                        .param("passengers", "99")
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
    }

    @Test
    void ownerProfileIs403() throws Exception { // solo arrendatario
        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID)
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
    }

    @Test
    void invalidUuidIs400() throws Exception { // identificador no UUID
        mockMvc.perform(get("/api/v1/vessels/not-a-uuid")
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));
    }

    @Test
    void invalidQuotationParametersAre400() throws Exception { // fechas invertidas o pasajeros no numericos
        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID + "/quotations")
                        .param("start_date", "2027-04-12")
                        .param("end_date", "2027-04-10")
                        .param("passengers", "4")
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID + "/quotations")
                        .param("start_date", "2027-04-10")
                        .param("end_date", "2027-04-12")
                        .param("passengers", "abc")
                        .header("X-User-Id", RENTER)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void missingIdentityIs401() throws Exception { // identidad temporal obligatoria
        mockMvc.perform(get("/api/v1/vessels/" + VESSEL_ID)
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
