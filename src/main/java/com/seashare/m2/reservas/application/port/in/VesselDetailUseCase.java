package com.seashare.m2.reservas.application.port.in;

import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselQuotation;

import java.time.LocalDate;
import java.util.UUID;

public interface VesselDetailUseCase { // puerto de entrada de CU-19

    VesselDetail getVesselDetail(UUID vesselId, String role); // ficha tecnica de la embarcacion

    VesselQuotation getQuotation(UUID vesselId, LocalDate startDate, LocalDate endDate, int passengers, String role); // cotizacion individual
}
