package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

final class ReservationTimeMapper { // utilidades de fechas y zona horaria para las vistas de consulta

    private ReservationTimeMapper() { // clase de utilidades
    }

    static ZoneId zoneOf(String timezone) { // resuelve la zona del puerto con fail-safe de presentacion
        if (timezone == null || timezone.isBlank()) { // reservas antiguas pueden no tener zona
            return ZoneOffset.UTC; // zona neutra documentada
        }
        try {
            return ZoneId.of(timezone.trim()); // zona IANA valida
        } catch (DateTimeException e) {
            return ZoneOffset.UTC; // zona invalida no rompe la vista
        }
    }

    static OffsetDateTime at(LocalDate date, LocalTime time, ZoneId zone) { // combina fecha y hora en la zona del puerto
        LocalTime effectiveTime = time == null ? LocalTime.MIDNIGHT : time; // sin hora pactada se usa medianoche
        return date.atTime(effectiveTime).atZone(zone).toOffsetDateTime(); // timestamp con offset
    }

    static int durationDays(LocalDate startDate, LocalDate endDate) { // dias inclusivos del rango
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1; // aritmetica de calendario, no monetaria
    }

    static int durationNights(LocalDate startDate, LocalDate endDate) { // noches del rango
        return (int) ChronoUnit.DAYS.between(startDate, endDate); // diferencia de fechas, no monetaria
    }
}
