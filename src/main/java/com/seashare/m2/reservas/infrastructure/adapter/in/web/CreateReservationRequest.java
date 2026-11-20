package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateReservationRequest(
        @JsonProperty("vessel_id")
        @NotNull
        UUID vesselId, // identificador de la embarcacion

        @JsonProperty("start_date")
        @NotNull
        LocalDate startDate, // fecha inicial de la reserva

        @JsonProperty("end_date")
        @NotNull
        LocalDate endDate, // fecha final de la reserva

        @JsonProperty("passengers")
        @NotNull
        @Positive
        Integer passengers, // numero positivo de pasajeros

        @JsonProperty("departure_time")
        @NotBlank
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
        String departureTime, // hora local del puerto con formato HH:mm

        // [NEEDS CLARIFICATION] formato y rango del monto no definidos en CU-02; se acepta decimal sin signo, hasta 14 enteros y 4 decimales (numeric(18,4)).
        @JsonProperty("estimated_total")
        @NotBlank
        @Pattern(regexp = "^\\d{1,14}(\\.\\d{1,4})?$")
        String estimatedTotal, // total estimado transportado como texto decimal

        @JsonProperty("holder")
        @NotNull
        @Valid
        Holder holder // datos obligatorios de contacto del titular
) { // payload json de entrada para solicitar la creacion de reserva

    public record Holder(
            @JsonProperty("full_name")
            @NotBlank
            @Size(max = 150)
            String fullName, // nombre completo del titular

            @JsonProperty("phone")
            @NotBlank
            @Size(max = 30)
            String phone, // celular o telefono de contacto

            @JsonProperty("email")
            @Email
            @Size(max = 150)
            String email // correo electronico opcional
    ) { // datos del titular de la reserva
    }
}
