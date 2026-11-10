package com.seashare.m2.shared.infrastructure.adapter.in.web;

import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-22: prueba minima del filtro de correlation id.
 * Verifica que la cabecera se respeta si llega y se genera si no llega.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainers.class)
class CorrelationIdFilterTest { // prueba del filtro X-Correlation-Id con MockMvc

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc con el filtro registrado

    @Test
    void respetaElCorrelationIdEntrante() throws Exception { // la cabecera del cliente se refleja en la respuesta
        mockMvc.perform(get("/api/v1/reservations") // endpoint de negocio: no toca Actuator ni RabbitMQ
                        .header(CorrelationIdFilter.HEADER, "corr-123")
                        .header("X-User-Id", UUID.randomUUID().toString()) // identidad temporal requerida
                        .header("X-User-Role", "RENTER")) // rol temporal requerido
                .andExpect(status().isOk()) // el endpoint responde 200
                .andExpect(header().string(CorrelationIdFilter.HEADER, "corr-123")); // id identico en la respuesta
    }

    @Test
    void generaCorrelationIdCuandoNoLlega() throws Exception { // sin cabecera se genera un UUID
        MvcResult result = mockMvc.perform(get("/api/v1/reservations") // sin cabecera de correlation
                        .header("X-User-Id", UUID.randomUUID().toString()) // identidad temporal requerida
                        .header("X-User-Role", "RENTER")) // rol temporal requerido
                .andExpect(status().isOk()) // el endpoint responde 200 sin depender del health de Rabbit
                .andReturn(); // captura la respuesta

        String generated = result.getResponse().getHeader(CorrelationIdFilter.HEADER); // cabecera reflejada
        assertThat(generated).isNotBlank(); // siempre presente
        assertThat(UUID.fromString(generated)).isNotNull(); // y con formato de UUID valido
    }
}
