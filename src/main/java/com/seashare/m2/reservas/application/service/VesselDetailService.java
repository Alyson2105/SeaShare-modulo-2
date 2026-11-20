package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.IndividualEstimate;
import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselQuotation;
import com.seashare.m2.reservas.application.port.in.VesselDetailUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.IndividualEstimatePort;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class VesselDetailService implements VesselDetailUseCase { // orquesta CU-09 y CU-11 para CU-19, sin persistir datos de flota

    private static final String CURRENCY = "COP"; // moneda constante de la plataforma (CU-11, M3 no la envia)

    private final VesselReadPort vesselReadPort; // ficha completa de modulo 1

    private final FleetInfoPort fleetInfoPort; // capacidad y semantica de errores de CU-09

    private final IndividualEstimatePort individualEstimatePort; // cotizacion individual de modulo 3

    public VesselDetailService(VesselReadPort vesselReadPort,
                               FleetInfoPort fleetInfoPort,
                               IndividualEstimatePort individualEstimatePort) { // inyeccion de puertos
        this.vesselReadPort = vesselReadPort; // asigna lectura de flota
        this.fleetInfoPort = fleetInfoPort; // asigna puerto de capacidad
        this.individualEstimatePort = individualEstimatePort; // asigna cotizacion
    }

    @Override
    public VesselDetail getVesselDetail(UUID vesselId, String role) { // ficha tecnica sin persistencia
        requireRenter(role); // FR-002 y seguridad del contrato
        VesselDetail detail = vesselReadPort.getDetail(vesselId); // 404/503 ya mapeados por el adaptador
        if (detail.maxCapacity() == null || detail.maxCapacity() <= 0) { // capacidad invalida = activo no disponible
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_FOUND, "La embarcación no está disponible para mostrar su detalle");
        }
        return detail; // retorna la ficha integra
    }

    @Override
    public VesselQuotation getQuotation(UUID vesselId,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        int passengers,
                                        String role) { // cotizacion individual con revalidacion de capacidad
        requireRenter(role); // solo arrendatario
        if (startDate == null || endDate == null) { // fechas obligatorias
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "start_date and end_date are required");
        }
        if (passengers <= 0) { // pasajeros positivos
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "passengers must be a positive integer");
        }
        if (endDate.isBefore(startDate)) { // rango cronologico basico; la validacion tarifaria sigue en M3
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "end_date cannot be before start_date");
        }

        FleetInfo fleetInfo = fleetInfoPort.getVesselInfo(vesselId); // revalida contra M1 (FR-004, SC-002)
        if (fleetInfo.maxCapacity() <= 0) { // capacidad invalida segun el edge case de CU-19
            throw new ReservationRuleException(ErrorCode.VESSEL_NOT_FOUND, "La embarcación no está disponible para cotizar");
        }
        if (passengers > fleetInfo.maxCapacity()) { // exceso de capacidad
            throw new ReservationRuleException(ErrorCode.CAPACITY_EXCEEDED,
                    "La cantidad de pasajeros (" + passengers + ") supera la capacidad máxima de la embarcación ("
                            + fleetInfo.maxCapacity() + ")");
        }

        IndividualEstimate estimate = individualEstimatePort.getIndividualEstimate(
                vesselId, startDate, endDate, passengers); // monto literal de M3, sin aritmetica
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1; // dias inclusivos del contrato de M3
        return new VesselQuotation(vesselId, startDate, endDate, totalDays, passengers,
                estimate.estimatedTotal(), CURRENCY, estimate.warning()); // advertencia literal
    }

    private void requireRenter(String role) { // el contrato de CU-19 autoriza solo al arrendatario
        if (role == null || !"RENTER".equalsIgnoreCase(role.trim())) {
            throw new ReservationRuleException(ErrorCode.PROFILE_NOT_AUTHORIZED, "Este recurso requiere perfil Arrendatario");
        }
    }
}
