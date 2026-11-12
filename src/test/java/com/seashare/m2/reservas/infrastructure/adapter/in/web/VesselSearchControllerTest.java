package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.CatalogVessel;
import com.seashare.m2.reservas.application.dto.VesselCatalogPage;
import com.seashare.m2.reservas.application.port.out.FleetCatalogPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.service.VesselSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VesselSearchControllerTest { // CU-01: contrato HTTP con catalogo deterministico

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ReservationRepositoryPort reservationRepository;

    private MockMvc mockMvc;

    private static List<CatalogVessel> catalog() { // 25 embarcaciones de prueba
        List<CatalogVessel> vessels = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            String type = i % 2 == 0 ? "YACHT" : "CATAMARAN";
            vessels.add(new CatalogVessel(
                    UUID.nameUUIDFromBytes(("vessel-" + i).getBytes(StandardCharsets.UTF_8)),
                    "Embarcacion " + i, type, "photo", "Marina", 11.0, -74.0,
                    "America/Bogota", 12, true));
        }
        return vessels;
    }

    @BeforeEach
    void setUp() {
        List<CatalogVessel> all = catalog();
        FleetCatalogPort catalogPort = (query, page, size) -> { // catalogo en memoria
            int from = Math.min((page - 1) * size, all.size());
            int to = Math.min(from + size, all.size());
            int totalPages = (int) Math.ceil(all.size() / (double) size);
            return new VesselCatalogPage(List.copyOf(all.subList(from, to)), page, size, all.size(), totalPages);
        };
        VesselSearchService service = new VesselSearchService(
                catalogPort,
                vesselIds -> new com.seashare.m2.reservas.application.dto.BatchEstimateResult(
                        vesselIds.stream().collect(java.util.stream.Collectors.toMap(
                                id -> id, id -> new java.math.BigDecimal("150000.00"))),
                        List.of()),
                reservationRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new VesselSearchController(service))
                .setCustomArgumentResolvers(new CallerIdentityArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchWithoutFiltersReturnsFirstPageOfTwenty() throws Exception { // FR-008 y User Story 2
        mockMvc.perform(get("/api/v1/vessels")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.page_size").value(20))
                .andExpect(jsonPath("$.pagination.total_items").value(25))
                .andExpect(jsonPath("$.pagination.total_pages").value(2))
                .andExpect(jsonPath("$.context_label").value("Populares esta semana"))
                .andExpect(jsonPath("$.vessels.length()").value(20))
                .andExpect(jsonPath("$.vessels[0].vessel_id").isNotEmpty())
                .andExpect(jsonPath("$.vessels[0].quotation_available").value(true))
                .andExpect(jsonPath("$.vessels[0].estimated_total").value(150000.00))
                .andExpect(jsonPath("$.vessels[0].currency").value("COP"));
    }

    @Test
    void filtersByTypeAndReturnsLaterPage() throws Exception { // FR-001 y paginacion
        mockMvc.perform(get("/api/v1/vessels")
                        .param("type", "YACHT")
                        .param("page", "2")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(2))
                .andExpect(jsonPath("$.vessels[*].type", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("YACHT"))));
    }

    @Test
    void invalidDateIsRejectedWithoutCallingModuleOne() throws Exception { // FR-010
        mockMvc.perform(get("/api/v1/vessels")
                        .param("start_date", "15-11-2026")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void invalidTypeIsRejected() throws Exception { // FR-010
        mockMvc.perform(get("/api/v1/vessels")
                        .param("type", "SUBMARINE")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void invalidPassengersIsRejected() throws Exception { // FR-010
        mockMvc.perform(get("/api/v1/vessels")
                        .param("passengers", "0")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void ownerIsNotAuthorized() throws Exception { // solo arrendatario
        mockMvc.perform(get("/api/v1/vessels")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_AUTHORIZED"));
    }

    @Test
    void overlappingReservationHidesVessel() throws Exception { // disponibilidad local
        when(reservationRepository.existsOverlap(any(), any(), any(), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/vessels")
                        .param("start_date", "2027-06-01")
                        .param("end_date", "2027-06-03")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vessels.length()").value(0))
                .andExpect(jsonPath("$.context_label").value("Sin resultados disponibles"));
    }
}
