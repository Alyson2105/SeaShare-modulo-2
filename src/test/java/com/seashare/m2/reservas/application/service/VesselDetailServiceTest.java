package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.IndividualEstimate;
import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselQuotation;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.IndividualEstimatePort;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VesselDetailServiceTest { // pruebas unitarias de CU-19 sin llamadas reales a M1 ni M3

    private static final UUID VESSEL_ID = UUID.fromString("d3b07384-d113-49cd-a5d6-812e9bcfc101"); // embarcacion fija

    private final VesselReadPort vesselReadPort = mock(VesselReadPort.class); // ficha de flota simulada

    private final FleetInfoPort fleetInfoPort = mock(FleetInfoPort.class); // capacidad simulada

    private final IndividualEstimatePort individualEstimatePort = mock(IndividualEstimatePort.class); // cotizacion simulada

    private final VesselDetailService service = new VesselDetailService(vesselReadPort, fleetInfoPort, individualEstimatePort);

    private VesselDetail detail(int capacity) { // ficha de prueba
        return new VesselDetail(VESSEL_ID, "Yate Tayrona", "REG-001", "YACHT", null, capacity, 48.5, 3, true,
                "Marina Santa Marta", null, 11.2443, -74.2125, "America/Bogota",
                List.of("GPS"), UUID.randomUUID(), "Inversiones Nauticas", true);
    }

    @Test
    void returnsVesselDetailForRenter() { // ficha completa para arrendatario
        when(vesselReadPort.getDetail(VESSEL_ID)).thenReturn(detail(12));

        VesselDetail result = service.getVesselDetail(VESSEL_ID, "RENTER");

        assertThat(result.name()).isEqualTo("Yate Tayrona");
        assertThat(result.maxCapacity()).isEqualTo(12);
        assertThat(result.ownerVerified()).isTrue();
    }

    @Test
    void invalidCapacityIsVesselNotFound() { // capacidad cero o nula aborta la ficha
        when(vesselReadPort.getDetail(VESSEL_ID)).thenReturn(detail(0));

        assertThatThrownBy(() -> service.getVesselDetail(VESSEL_ID, "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode()));
    }

    @Test
    void ownerProfileIsRejected() { // CU-19 exige perfil arrendatario
        assertThatThrownBy(() -> service.getVesselDetail(VESSEL_ID, "OWNER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
        verify(vesselReadPort, never()).getDetail(any());
    }

    @Test
    void quotationUsesLiteralAmountAndInclusiveDays() { // monto y advertencia literales de M3
        LocalDate start = LocalDate.of(2027, 4, 10);
        LocalDate end = LocalDate.of(2027, 4, 12);
        when(fleetInfoPort.getVesselInfo(VESSEL_ID)).thenReturn(new FleetInfo(UUID.randomUUID(), 12, "America/Bogota"));
        when(individualEstimatePort.getIndividualEstimate(VESSEL_ID, start, end, 4))
                .thenReturn(new IndividualEstimate(new BigDecimal("9720000.00"), "Valor estimado literal"));

        VesselQuotation quotation = service.getQuotation(VESSEL_ID, start, end, 4, "RENTER");

        assertThat(quotation.totalAmount()).isEqualByComparingTo("9720000.00");
        assertThat(quotation.warning()).isEqualTo("Valor estimado literal");
        assertThat(quotation.currency()).isEqualTo("COP");
        assertThat(quotation.totalDays()).isEqualTo(3); // 10, 11 y 12 de abril
    }

    @Test
    void quotationAcceptsEqualDatesAsOneInclusiveDay() { // CU-11 admite un solo dia; CU-19 no lo rechaza
        LocalDate day = LocalDate.of(2027, 4, 10);
        when(fleetInfoPort.getVesselInfo(VESSEL_ID)).thenReturn(new FleetInfo(UUID.randomUUID(), 12, "America/Bogota"));
        when(individualEstimatePort.getIndividualEstimate(VESSEL_ID, day, day, 4))
                .thenReturn(new IndividualEstimate(new BigDecimal("3240000.00"), "Valor estimado literal"));

        VesselQuotation quotation = service.getQuotation(VESSEL_ID, day, day, 4, "RENTER");

        assertThat(quotation.totalDays()).isEqualTo(1); // dia inclusivo
        assertThat(quotation.totalAmount()).isEqualByComparingTo("3240000.00");
    }

    @Test
    void capacityExceededIsRejectedBeforeM3() { // se revalida contra M1 antes de cotizar
        when(fleetInfoPort.getVesselInfo(VESSEL_ID)).thenReturn(new FleetInfo(UUID.randomUUID(), 6, "America/Bogota"));

        assertThatThrownBy(() -> service.getQuotation(VESSEL_ID, LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12), 8, "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.CAPACITY_EXCEEDED.getCode()));
        verify(individualEstimatePort, never()).getIndividualEstimate(any(), any(), any(), anyInt());
    }

    @Test
    void invalidDatesOrPassengersAreRejected() { // validacion basica de entrada
        assertThatThrownBy(() -> service.getQuotation(VESSEL_ID, LocalDate.of(2027, 4, 12),
                LocalDate.of(2027, 4, 10), 2, "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        assertThatThrownBy(() -> service.getQuotation(VESSEL_ID, LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12), 0, "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        verify(fleetInfoPort, never()).getVesselInfo(any());
    }

    @Test
    void fleetFailurePropagates() { // 404 de M1 conserva su codigo
        when(fleetInfoPort.getVesselInfo(eq(VESSEL_ID)))
                .thenThrow(new ReservationRuleException(ErrorCode.VESSEL_NOT_FOUND, "no existe"));

        assertThatThrownBy(() -> service.getQuotation(VESSEL_ID, LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12), 2, "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.VESSEL_NOT_FOUND.getCode()));
    }

    @Test
    void financeFailurePropagates() { // fallo de M3 conserva QUOTATION_UNAVAILABLE
        when(fleetInfoPort.getVesselInfo(VESSEL_ID)).thenReturn(new FleetInfo(UUID.randomUUID(), 12, "America/Bogota"));
        when(individualEstimatePort.getIndividualEstimate(any(), any(), any(), anyInt()))
                .thenThrow(new ReservationRuleException(ErrorCode.QUOTATION_UNAVAILABLE, "M3 caido"));

        assertThatThrownBy(() -> service.getQuotation(VESSEL_ID, LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12), 2, "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.QUOTATION_UNAVAILABLE.getCode()));
    }
}
