package com.seashare.m2.shared.domain;

import java.util.HashMap;
import java.util.Map;

public enum ErrorCode { // catalogo de codigos de error con mapeo http

    INVALID_PARAMETERS("INVALID_PARAMETERS", 400), // parametros de solicitud invalidos
    INVALID_UUID("INVALID_UUID", 400), // identificador uuid invalido
    UNAUTHENTICATED("UNAUTHENTICATED", 401), // usuario no autenticado
    PROFILE_NOT_AUTHORIZED("PROFILE_NOT_AUTHORIZED", 403), // perfil sin autorizacion
    RESERVATION_NOT_OWNED("RESERVATION_NOT_OWNED", 403), // reserva no pertenece al usuario
    RESERVATION_NOT_FOUND("RESERVATION_NOT_FOUND", 404), // reserva no encontrada
    VESSEL_NOT_FOUND("VESSEL_NOT_FOUND", 404), // embarcacion no encontrada
    INVALID_RESERVATION_STATE("INVALID_RESERVATION_STATE", 409), // estado de reserva invalido
    CONCURRENT_STATE_CHANGE("CONCURRENT_STATE_CHANGE", 409), // cambio concurrente de estado
    CONCURRENCY_CONFLICT("CONCURRENCY_CONFLICT", 409), // conflicto de concurrencia
    CAPACITY_EXCEEDED("CAPACITY_EXCEEDED", 409), // capacidad de la embarcacion superada
    VESSEL_NOT_AVAILABLE("VESSEL_NOT_AVAILABLE", 409), // embarcacion no disponible
    VESSEL_CURRENTLY_IN_NAVIGATION("VESSEL_CURRENTLY_IN_NAVIGATION", 409), // embarcacion en navegacion actualmente
    RESERVATION_EXPIRED("RESERVATION_EXPIRED", 409), // reserva expirada por ttl
    RESERVATION_EXPIRED_REVERSION_REQUIRED("RESERVATION_EXPIRED_REVERSION_REQUIRED", 409), // reserva expirada requiere reversion
    DISPUTE_ALREADY_RESOLVED("DISPUTE_ALREADY_RESOLVED", 409), // disputa ya resuelta
    INTERNAL_ERROR("INTERNAL_ERROR", 500), // error interno del servidor
    FLEET_SERVICE_UNAVAILABLE("FLEET_SERVICE_UNAVAILABLE", 503), // servicio de flota no disponible
    HARBOR_TIMEZONE_UNAVAILABLE("HARBOR_TIMEZONE_UNAVAILABLE", 503), // zona horaria del puerto no disponible
    QUOTATION_UNAVAILABLE("QUOTATION_UNAVAILABLE", 503); // cotizacion no disponible

    private static final Map<String, ErrorCode> BY_CODE = new HashMap<>(); // indice por codigo

    static {
        for (ErrorCode errorCode : values()) { // registra cada valor del catalogo
            BY_CODE.put(errorCode.code, errorCode); // asocia codigo con la constante
        }
    }

    private final String code; // codigo identificador del error
    private final int httpStatus; // codigo de estado http asociado

    ErrorCode(String code, int httpStatus) { // constructor del catalogo
        this.code = code; // asigna codigo
        this.httpStatus = httpStatus; // asigna estado http
    }

    public String getCode() { // obtiene el codigo del error
        return code; // retorna el codigo
    }

    public int getHttpStatus() { // obtiene el estado http
        return httpStatus; // retorna el codigo de estado http
    }

    public static ErrorCode fromInternal(String code) { // traduce codigo interno a codigo publico
        if (code == null) { // maneja valor nulo
            return INTERNAL_ERROR; // retorna error interno por defecto
        }
        if ("INVALID_STATE_TRANSITION".equals(code)) { // mapeo de transicion invalida
            return INVALID_RESERVATION_STATE; // retorna estado de reserva invalido
        }
        if ("VERSION_CONFLICT".equals(code)) { // mapeo de conflicto de version
            return CONCURRENT_STATE_CHANGE; // retorna cambio concurrente
        }
        ErrorCode match = BY_CODE.get(code); // busca en el indice
        return match != null ? match : INTERNAL_ERROR; // retorna coincidencia o error interno
    }
}
