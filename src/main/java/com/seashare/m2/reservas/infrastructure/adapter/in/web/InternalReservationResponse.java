package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record InternalReservationResponse(
        @JsonProperty("reservation_id")
        UUID reservationId, // identificador de la reserva

        @JsonProperty("reservation_code")
        String reservationCode, // codigo de negocio

        @JsonProperty("renter_id")
        UUID renterId, // arrendatario

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion

        @JsonProperty("owner_id")
        UUID ownerId, // propietario

        @JsonProperty("dates")
        Dates dates, // itinerario pactado y duraciones

        @JsonProperty("passengers")
        int passengers, // pasajeros autorizados

        @JsonProperty("primary_status")
        ReservationStatus primaryStatus, // estado interno vigente

        @JsonProperty("cancellation_subtype")
        CancellationSubtype cancellationSubtype, // subtipo de cancelacion

        @JsonProperty("cancellation_actor")
        String cancellationActor, // RENTER, OWNER o SYSTEM_TTL

        @JsonProperty("cancellation_anticipation_hours")
        BigDecimal cancellationAnticipationHours, // horas de anticipacion registradas por CU-04

        @JsonProperty("checkin_checkout")
        CheckinCheckout checkinCheckout, // datos reales de muelle

        @JsonProperty("ttl_timer")
        TtlTimer ttlTimer, // estado del temporizador de pago

        @JsonProperty("created_at")
        String createdAt, // creacion en UTC

        @JsonProperty("updated_at")
        String updatedAt // ultima actualizacion en UTC
) { // respuesta snake_case del endpoint interno de Modulo 3

    // Formato ISO con offset y segundos siempre visibles, por ejemplo 2027-05-10T09:00:00-05:00.
    private static final DateTimeFormatter OFFSET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public record Dates(
            @JsonProperty("scheduled_departure")
            String scheduledDeparture, // zarpe pactado con offset

            @JsonProperty("scheduled_arrival")
            String scheduledArrival, // desembarque pactado con offset

            @JsonProperty("timezone")
            String timezone, // zona horaria del puerto

            @JsonProperty("duration_days")
            int durationDays, // dias completos

            @JsonProperty("duration_nights")
            int durationNights // noches
    ) { // bloque de fechas pactadas
    }

    public record CheckinCheckout(
            @JsonProperty("checkin_at")
            String checkinAt, // check-in real en UTC

            @JsonProperty("checkout_at")
            String checkoutAt, // check-out real en UTC

            @JsonProperty("reported_remarks")
            String reportedRemarks, // novedades de cierre

            @JsonProperty("damage_detected")
            Boolean damageDetected // siempre nulo hasta que exista la columna
    ) { // bloque de operacion real en muelle
    }

    public record TtlTimer(
            @JsonProperty("applies")
            boolean applies, // verdadero solo en el embudo de pago

            @JsonProperty("expires_at")
            String expiresAt, // vencimiento del ttl en UTC

            @JsonProperty("remaining_seconds")
            Long remainingSeconds // segundos restantes; cero si no aplica
    ) { // bloque del temporizador de pago
    }

    public static InternalReservationResponse from(InternalReservationView view) { // fabrica de respuesta a partir de la vista
        ZoneId zoneId = resolveZone(view.harborTimezone()); // zona del puerto o nula si es invalida

        return new InternalReservationResponse(
                view.reservationId(), // id
                view.reservationCode(), // codigo
                view.renterId(), // arrendatario
                view.vesselId(), // embarcacion
                view.ownerId(), // propietario
                new Dates(
                        formatDeparture(view.startDate(), view.departureTime(), zoneId), // zarpe pactado
                        formatDeparture(view.endDate(), view.departureTime(), zoneId), // regreso pactado (provisional)
                        zoneId == null ? null : view.harborTimezone(), // zona horaria valida o nula
                        view.durationDays(), // dias
                        view.durationNights()), // noches
                view.passengers(), // pasajeros
                view.status(), // estado interno
                view.cancellationSubtype(), // subtipo
                view.cancellationActor(), // actor de cancelacion
                anticipationHours(view.cancellationAnticipationMinutes()), // horas reales o nulas sin evento
                new CheckinCheckout(
                        instantText(view.actualDepartureAt()), // check-in real
                        instantText(view.actualArrivalAt()), // check-out real
                        view.completionNotes(), // novedades
                        // CU-07 (fin de navegacion) aun no persiste damage_detected: se informa nulo, nunca un valor supuesto.
                        null), // damage_detected: nulo hasta que CU-07 lo capture
                new TtlTimer(
                        view.ttlApplies(), // aplica o no
                        view.ttlApplies() ? instantText(view.ttlExpiresAt()) : null, // vencimiento solo si aplica
                        view.ttlRemainingSeconds()), // segundos restantes
                instantText(view.createdAt()), // creacion
                instantText(view.updatedAt()) // actualizacion
        );
    }

    private static BigDecimal anticipationHours(Long anticipationMinutes) { // convierte minutos persistidos a horas del contrato
        if (anticipationMinutes == null) { // sin evento de cancelacion
            return null; // se informa nulo
        }
        return BigDecimal.valueOf(anticipationMinutes / 60.0); // conversion temporal, sin redondear a dos decimales
    }

    private static ZoneId resolveZone(String timezone) { // valida la zona horaria sin aplicar valores por defecto
        if (timezone == null || timezone.isBlank()) { // zona ausente
            return null; // sin zona, los horarios pactados quedan nulos
        }
        try {
            return ZoneId.of(timezone); // resuelve la zona del puerto
        } catch (RuntimeException e) { // zona desconocida
            return null; // sin zona por defecto
        }
    }

    private static String formatDeparture(LocalDate date, LocalTime time, ZoneId zoneId) { // compone fecha y hora pactadas
        if (zoneId == null || date == null || time == null) { // faltan datos para el horario
            return null; // se informa nulo
        }
        // [NEEDS CLARIFICATION] M2 no guarda hora de llegada: la llegada se compone con end_date y departure_time.
        return date.atTime(time).atZone(zoneId).toOffsetDateTime().format(OFFSET_FORMATTER); // texto ISO con offset
    }

    private static String instantText(java.time.Instant instant) { // serializa instantes en UTC
        return instant == null ? null : instant.toString(); // formato ISO-8601 UTC
    }
}
