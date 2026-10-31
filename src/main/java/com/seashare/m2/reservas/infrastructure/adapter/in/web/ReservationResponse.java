package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.port.out.ReservationView;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.service.TtlPolicy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ReservationResponse(
        @JsonProperty("reservation_id")
        UUID reservationId, // identificador unico de la reserva

        @JsonProperty("status")
        ReservationStatus status, // estado actual de la reserva

        @JsonProperty("vessel_id")
        UUID vesselId, // identificador de la embarcacion

        @JsonProperty("start_date")
        LocalDate startDate, // fecha inicial de la reserva

        @JsonProperty("end_date")
        LocalDate endDate, // fecha final de la reserva

        @JsonProperty("passengers")
        int passengers, // cantidad de pasajeros

        @JsonProperty("holder")
        HolderResponse holder, // datos de contacto del titular

        @JsonProperty("estimated_total")
        String estimatedTotal, // total estimado devuelto como texto con toPlainString

        @JsonProperty("currency")
        String currency, // codigo de moneda

        @JsonProperty("departure_time")
        String departureTime, // hora local formateada como HH:mm

        @JsonProperty("ttl_minutes")
        long ttlMinutes, // duracion en minutos del temporizador ttl

        @JsonProperty("ttl_expires_at")
        OffsetDateTime ttlExpiresAt, // timestamp de expiracion en la zona horaria del puerto

        @JsonProperty("created_at")
        OffsetDateTime createdAt // timestamp de creacion en la zona horaria del puerto
) { // payload json de respuesta exitosa 201 Created

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm"); // formateador para hora de salida

    public record HolderResponse(
            @JsonProperty("full_name")
            String fullName, // nombre completo del titular

            @JsonProperty("phone")
            String phone, // celular de contacto del titular

            @JsonProperty("email")
            String email // correo electronico del titular
    ) { // datos anidados del titular de la reserva
    }

    public static ReservationResponse from(ReservationView view) { // fabrica de respuesta a partir de la vista de reserva
        ZoneId zoneId = ZoneId.of(view.harborTimezone()); // resuelve zona horaria validada directamente

        OffsetDateTime createdAt = view.createdAt().atZone(zoneId).toOffsetDateTime(); // convierte creacion a zona del puerto
        OffsetDateTime ttlExpiresAt = view.ttlExpiresAt().atZone(zoneId).toOffsetDateTime(); // convierte expiracion a zona del puerto

        return new ReservationResponse(
                view.reservationId(), // id de la reserva
                view.status(), // estado INITIATED
                view.vesselId(), // embarcacion
                view.startDate(), // fecha de inicio
                view.endDate(), // fecha de fin
                view.passengers(), // pasajeros
                new HolderResponse(view.holderName(), view.holderPhone(), view.holderEmail()), // titular
                view.estimatedTotal().toPlainString(), // total estimado sin notacion cientifica
                view.currency(), // moneda
                view.departureTime().format(TIME_FORMATTER), // hora formateada HH:mm
                TtlPolicy.TTL.toMinutes(), // quince minutos de vigencia
                ttlExpiresAt, // expiracion con offset
                createdAt // creacion con offset
        );
    }
}
