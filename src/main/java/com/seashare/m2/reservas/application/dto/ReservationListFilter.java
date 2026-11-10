package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ErrorCode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum ReservationListFilter { // filtro por pestana del listado de CU-20

    ALL("all", null), // universo completo del usuario

    HISTORY("history", Collections.unmodifiableSet(EnumSet.of( // reservas activas o completadas
            ReservationStatus.INITIATED,
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.IN_NAVIGATION,
            ReservationStatus.COMPLETED))),

    CANCELLED("cancelled", Collections.unmodifiableSet(EnumSet.of( // reservas terminadas prematuramente
            ReservationStatus.CANCELLED,
            ReservationStatus.EXPIRED,
            ReservationStatus.PAYMENT_FAILED)));

    private final String externalValue; // valor recibido en la query string

    private final Set<ReservationStatus> statuses; // estados que pertenecen al filtro; null = todos

    ReservationListFilter(String externalValue, Set<ReservationStatus> statuses) {
        this.externalValue = externalValue; // asigna valor externo
        this.statuses = statuses; // asigna conjunto de estados
    }

    public String externalValue() { // expone el valor aplicado en la respuesta
        return externalValue; // retorna el texto del filtro
    }

    public Set<ReservationStatus> statuses() { // expone los estados del filtro
        return statuses; // null significa sin restriccion
    }

    public static ReservationListFilter from(String value) { // traduce la query string al filtro
        if (value == null || value.isBlank()) { // ausencia del parametro equivale a all
            return ALL; // filtro por defecto
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT); // normaliza para aceptar mayusculas
        for (ReservationListFilter filter : values()) { // recorre los filtros soportados
            if (filter.externalValue.equals(normalized)) { // coincide con el valor externo
                return filter; // retorna el filtro
            }
        }
        throw new ReservationRuleException(ErrorCode.INVALID_PARAMETERS, "Unsupported filter value: " + value); // rechaza filtros no soportados
    }
}
