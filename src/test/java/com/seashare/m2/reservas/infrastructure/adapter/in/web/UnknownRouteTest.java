package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestContainers.class)
class UnknownRouteTest { // rutas inexistentes responden 404 uniforme

    @Autowired
    private MockMvc mockMvc; // cliente HTTP

    @Test
    void unknownRouteReturns404WithStandardBody() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
