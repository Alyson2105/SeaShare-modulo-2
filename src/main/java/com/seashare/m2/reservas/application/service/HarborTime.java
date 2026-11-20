package com.seashare.m2.reservas.application.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class HarborTime { // utilidades de fecha y zona horaria del puerto

    private HarborTime() { // clase de utilidades
    }

    public static ZoneId zoneOf(String timezone) { // resuelve la zona del puerto con fail-safe de presentacion
        if (timezone == null || timezone.isBlank()) { // reservas antiguas pueden no tener zona
            return ZoneOffset.UTC; // zona neutra documentada
        }
        try {
            return ZoneId.of(timezone.trim()); // zona IANA valida
        } catch (DateTimeException e) {
            return ZoneOffset.UTC; // zona invalida no rompe la vista
        }
    }

    public static OffsetDateTime at(LocalDate date, LocalTime time, ZoneId zone) { // combina fecha y hora en la zona del puerto
        LocalTime effectiveTime = time == null ? LocalTime.MIDNIGHT : time; // sin hora pactada se usa medianoche
        return date.atTime(effectiveTime).atZone(zone).toOffsetDateTime(); // timestamp con offset
    }

    public static int durationDays(LocalDate startDate, LocalDate endDate) { // dias inclusivos del rango
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1; // aritmetica de calendario, no monetaria
    }

    public static int durationNights(LocalDate startDate, LocalDate endDate) { // noches del rango
        return (int) ChronoUnit.DAYS.between(startDate, endDate); // diferencia de fechas, no monetaria
    }
}
