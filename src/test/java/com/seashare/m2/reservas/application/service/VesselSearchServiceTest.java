package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.BatchEstimateResult;
import com.seashare.m2.reservas.application.dto.CatalogVessel;
import com.seashare.m2.reservas.application.dto.VesselCatalogPage;
import com.seashare.m2.reservas.application.dto.VesselSearchCriteria;
import com.seashare.m2.reservas.application.dto.VesselSearchResult;
import com.seashare.m2.reservas.application.port.out.BatchEstimatePort;
import com.seashare.m2.reservas.application.port.out.FleetCatalogPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VesselSearchServiceTest { // CU-01: filtros, degradacion y paginacion

    private static final UUID YACHT_ID = UUID.randomUUID();

    private static final UUID CATAMARAN_ID = UUID.randomUUID();

    private static final LocalDate START = LocalDate.of(2027, 6, 1);

    private static final LocalDate END = LocalDate.of(2027, 6, 3);

    @Mock
    private FleetCatalogPort catalogPort;

    @Mock
    private BatchEstimatePort batchEstimatePort;

    @Mock
    private ReservationRepositoryPort reservationRepository;

    private VesselSearchService service;

    @BeforeEach
    void setUp() {
        service = new VesselSearchService(catalogPort, batchEstimatePort, reservationRepository);
    }

    private CatalogVessel vessel(UUID id, String type, int capacity) {
        return new CatalogVessel(id, "Embarcacion", type, "photo", "Marina", 11.0, -74.0,
                "America/Bogota", capacity, true);
    }

    private VesselCatalogPage page(List<CatalogVessel> vessels, long total) {
        return new VesselCatalogPage(vessels, 1, 20, total, (int) Math.ceil(total / 20.0));
    }

    @Test
    void nonRenterCannotSearch() { // solo el arrendatario explora el catalogo
        VesselSearchCriteria criteria = new VesselSearchCriteria(null, null, null, null, null, 1);

        assertThatThrownBy(() -> service.search(criteria, "OWNER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));

        verify(catalogPort, never()).search(any(), anyInt(), anyInt()); // no se consulta M1
    }

    @Test
    void filtersByTypeLocally() { // M1 no filtra por tipo
        when(catalogPort.search(null, 1, 20)).thenReturn(page(List.of(
                vessel(YACHT_ID, "YACHT", 10), vessel(CATAMARAN_ID, "CATAMARAN", 10)), 2));
        when(batchEstimatePort.getBatchEstimates(anyList())).thenReturn(
                new BatchEstimateResult(Map.of(YACHT_ID, new BigDecimal("100.00")), List.of()));

        VesselSearchResult result = service.search(
                new VesselSearchCriteria(null, null, null, "YACHT", null, 1), "RENTER");

        assertThat(result.vessels()).extracting(CatalogVessel::vesselId).containsExactly(YACHT_ID);
        assertThat(result.estimates()).containsEntry(YACHT_ID, new BigDecimal("100.00"));
    }

    @Test
    void filtersByCapacity() { // FR-001: pasajeros requeridos
        when(catalogPort.search(null, 1, 20)).thenReturn(page(List.of(
                vessel(YACHT_ID, "YACHT", 6), vessel(CATAMARAN_ID, "CATAMARAN", 14)), 2));
        when(batchEstimatePort.getBatchEstimates(List.of(CATAMARAN_ID))).thenReturn(
                new BatchEstimateResult(Map.of(CATAMARAN_ID, new BigDecimal("200.00")), List.of()));

        VesselSearchResult result = service.search(
                new VesselSearchCriteria(null, null, 10, null, null, 1), "RENTER");

        assertThat(result.vessels()).extracting(CatalogVessel::vesselId).containsExactly(CATAMARAN_ID);
    }

    @Test
    void excludesVesselsWithOverlappingReservations() { // disponibilidad local por fechas
        when(catalogPort.search(null, 1, 20)).thenReturn(page(List.of(
                vessel(YACHT_ID, "YACHT", 10), vessel(CATAMARAN_ID, "CATAMARAN", 10)), 2));
        when(reservationRepository.existsOverlap(eq(YACHT_ID), eq(START), eq(END), any())).thenReturn(true);
        when(reservationRepository.existsOverlap(eq(CATAMARAN_ID), eq(START), eq(END), any())).thenReturn(false);
        when(batchEstimatePort.getBatchEstimates(List.of(CATAMARAN_ID))).thenReturn(
                new BatchEstimateResult(Map.of(CATAMARAN_ID, new BigDecimal("200.00")), List.of()));

        VesselSearchResult result = service.search(
                new VesselSearchCriteria(START, END, null, null, null, 1), "RENTER");

        assertThat(result.vessels()).extracting(CatalogVessel::vesselId).containsExactly(CATAMARAN_ID);
    }

    @Test
    void noFiltersReturnsWholeCatalogWithDefaultLabel() { // User Story 2
        when(catalogPort.search(null, 1, 20)).thenReturn(page(List.of(vessel(YACHT_ID, "YACHT", 10)), 25));
        when(batchEstimatePort.getBatchEstimates(anyList())).thenReturn(
                new BatchEstimateResult(Map.of(YACHT_ID, new BigDecimal("100.00")), List.of()));

        VesselSearchResult result = service.search(
                new VesselSearchCriteria(null, null, null, null, null, 1), "RENTER");

        assertThat(result.contextLabel()).isEqualTo("Populares esta semana");
        assertThat(result.totalItems()).isEqualTo(25);
    }

    @Test
    void emptyResultDoesNotQueryModuleThree() { // sin ids no se invoca M3
        when(catalogPort.search("Nada", 1, 20)).thenReturn(page(List.of(), 0));

        VesselSearchResult result = service.search(
                new VesselSearchCriteria(null, null, null, null, "Nada", 1), "RENTER");

        assertThat(result.contextLabel()).isEqualTo("Sin resultados disponibles");
        assertThat(result.vessels()).isEmpty();
        verify(batchEstimatePort, never()).getBatchEstimates(any()); // sin cotizacion
    }

    @Test
    void quotationUnavailableDegradesWithoutFailing() { // cotizacion ausente por contingencia de M3
        when(catalogPort.search(null, 1, 20)).thenReturn(page(List.of(vessel(YACHT_ID, "YACHT", 10)), 1));
        when(batchEstimatePort.getBatchEstimates(anyList())).thenReturn(
                new BatchEstimateResult(Map.of(), List.of(YACHT_ID)));

        VesselSearchResult result = service.search(
                new VesselSearchCriteria(null, null, null, null, null, 1), "RENTER");

        assertThat(result.vessels()).hasSize(1);
        assertThat(result.estimates()).isEmpty();
        assertThat(result.contextLabel()).isEqualTo("Catálogo general (Tarifas en actualización)");
    }
}
