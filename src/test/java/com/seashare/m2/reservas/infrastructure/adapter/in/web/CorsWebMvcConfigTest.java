package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestContainers.class)
class CorsWebMvcConfigTest { // CORS hacia el frontend (B-01), misma base que UnknownRouteTest

    private static final String APP_ORIGIN = "http://localhost:5173"; // origen del frontend en dev

    @Autowired
    private MockMvc mockMvc; // cliente HTTP

    @Test
    void preflightFromAllowedOriginReturnsAllowedHeaders() throws Exception { // a: preflight valido
        MvcResult result = mockMvc.perform(options("/api/v1/reservations")
                        .header("Origin", APP_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers",
                                "x-user-id,x-user-role,idempotency-key,x-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", APP_ORIGIN))
                .andReturn();

        String allowHeaders = result.getResponse().getHeader("Access-Control-Allow-Headers");
        assertThat(allowHeaders).isNotNull(); // el preflight siempre responde la lista
        String lower = allowHeaders.toLowerCase(Locale.ROOT); // el navegador compara sin importar mayusculas
        assertThat(lower).contains("x-user-id", "x-user-role", "idempotency-key", "x-correlation-id");
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception { // b: origen fuera de la lista blanca
        mockMvc.perform(options("/api/v1/reservations")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "x-user-id"))
                .andExpect(status().isForbidden());
    }

    @Test
    void realGetExposesCorrelationHeader() throws Exception { // c: expose-headers en una peticion real
        mockMvc.perform(get("/api/v1/vessels")
                        .header("Origin", APP_ORIGIN)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers",
                        containsString("X-Correlation-Id")));
    }
}
