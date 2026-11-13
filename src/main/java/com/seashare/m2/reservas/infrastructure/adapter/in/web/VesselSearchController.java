package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.dto.VesselSearchCriteria;
import com.seashare.m2.reservas.application.dto.VesselSearchResult;
import com.seashare.m2.reservas.application.port.in.VesselSearchUseCase;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.shared.domain.ErrorCode;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/vessels")
public class VesselSearchController { // CU-01: busqueda y exploracion del catalogo

    private final VesselSearchUseCase vesselSearchUseCase; // caso de uso de busqueda

    public VesselSearchController(VesselSearchUseCase vesselSearchUseCase) {
        this.vesselSearchUseCase = vesselSearchUseCase; // asigna caso de uso
    }

    @GetMapping
    public ResponseEntity<VesselSearchResponse> search(
            CallerIdentity identity, // identidad temporal por headers
            @RequestParam(name = "start_date", required = false) String startDate, // fecha ISO opcional
            @RequestParam(name = "end_date", required = false) String endDate, // fecha ISO opcional
            @RequestParam(name = "passengers", required = false) String passengers, // entero opcional
            @RequestParam(name = "type", required = false) String type, // categoria opcional
            @RequestParam(name = "q", required = false) String query, // texto libre opcional
            @RequestParam(name = "page", defaultValue = "1") String page) { // pagina, base 1
        VesselSearchCriteria criteria = new VesselSearchCriteria(
                parseDate(startDate, "start_date"),
                parseDate(endDate, "end_date"),
                parsePassengers(passengers),
                parseType(type),
                query,
                parsePage(page));

        VesselSearchResult result = vesselSearchUseCase.search(criteria, identity.role()); // FR-010: validado antes de M1/M3
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore()) // informacion en tiempo real
                .body(VesselSearchResponse.from(result));
    }

    private LocalDate parseDate(String value, String field) { // solo valida formato (FR-010)
        if (value == null || value.isBlank()) {
            return null; // criterio opcional
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    field + " must use ISO 8601 format YYYY-MM-DD"); // rechaza sin consultar M1 ni M3
        }
    }

    private Integer parsePassengers(String value) { // entero positivo opcional
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException("non positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "passengers must be a positive integer");
        }
    }

    private String parseType(String value) { // categoria del catalogo
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!com.seashare.m2.reservas.application.service.VesselSearchService.ALLOWED_TYPES.contains(normalized)) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "type must be one of MOTORBOAT, SAILBOAT, YACHT or CATAMARAN"); // FR-010
        }
        return normalized;
    }

    private int parsePage(String value) { // pagina mayor o igual a uno
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1) {
                throw new NumberFormatException("page below one");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS,
                    "page must be an integer greater than or equal to 1");
        }
    }
}
