package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselQuotation;
import com.seashare.m2.reservas.application.port.in.VesselDetailUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vessels")
public class VesselDetailController { // endpoints de CU-19, sin persistencia de datos de flota

    private final VesselDetailUseCase vesselDetailUseCase; // caso de uso de ficha y cotizacion

    public VesselDetailController(VesselDetailUseCase vesselDetailUseCase) { // inyeccion del caso de uso
        this.vesselDetailUseCase = vesselDetailUseCase; // asigna caso de uso
    }

    @GetMapping("/{vessel_id}")
    public ResponseEntity<VesselDetailResponse> getVessel(
            CallerIdentity identity, // identidad temporal por headers
            @PathVariable("vessel_id") String vesselId // identificador como texto para validar formato
    ) { // Endpoint 1: ficha tecnica de la embarcacion
        UUID id = parseUuid(vesselId); // valida UUID
        VesselDetail detail = vesselDetailUseCase.getVesselDetail(id, identity.role()); // consulta M1 en tiempo real
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore()) // informacion en tiempo real (contrato CU-19)
                .body(VesselDetailResponse.from(detail)); // respuesta sin persistencia
    }

    @GetMapping("/{vessel_id}/quotations")
    public ResponseEntity<VesselQuotationResponse> getQuotation(
            CallerIdentity identity, // identidad temporal por headers
            @PathVariable("vessel_id") String vesselId, // identificador como texto
            @RequestParam("start_date") String startDate, // fecha ISO YYYY-MM-DD
            @RequestParam("end_date") String endDate, // fecha ISO YYYY-MM-DD
            @RequestParam("passengers") String passengers // entero positivo como texto para validar
    ) { // Endpoint 2: cotizacion individual para el detalle
        UUID id = parseUuid(vesselId); // valida UUID
        LocalDate start = parseDate(startDate, "start_date"); // valida fecha de inicio
        LocalDate end = parseDate(endDate, "end_date"); // valida fecha de fin
        int passengerCount = parsePassengers(passengers); // valida pasajeros
        VesselQuotation quotation = vesselDetailUseCase.getQuotation(id, start, end, passengerCount, identity.role()); // M1 + M3
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore()) // cotizacion dinamica, nunca cacheada
                .body(VesselQuotationResponse.from(quotation)); // sin calculos monetarios
    }

    private static UUID parseUuid(String raw) { // valida el formato del identificador
        try {
            return UUID.fromString(raw == null ? "" : raw.trim()); // parseo canonico
        } catch (IllegalArgumentException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_UUID,
                    "El identificador de embarcación proporcionado no es un UUID válido"); // 400
        }
    }

    private static LocalDate parseDate(String raw, String field) { // valida fechas ISO
        try {
            return LocalDate.parse(raw == null ? "" : raw.trim()); // YYYY-MM-DD
        } catch (DateTimeParseException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "El parámetro '" + field + "' debe tener formato ISO YYYY-MM-DD"); // 400
        }
    }

    private static int parsePassengers(String raw) { // valida el entero de pasajeros
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim()); // parseo numerico
        } catch (NumberFormatException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "La cantidad de pasajeros debe ser un número entero"); // 400
        }
    }
}
