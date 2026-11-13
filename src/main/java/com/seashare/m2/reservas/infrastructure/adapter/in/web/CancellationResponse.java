package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.CancellationResult;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.OperationalStatus;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record CancellationResponse( // respuesta exitosa de CU-04

        @JsonProperty("reservation_id")
        UUID reservationId, // reserva cancelada

        @JsonProperty("status")
        String status, // estado terminal

        @JsonProperty("cancellation_subtype")
        String cancellationSubtype, // clasificacion aplicada

        @JsonProperty("requesting_actor")
        String requestingActor, // actor que solicito la cancelacion

        @JsonProperty("anticipation_hours")
        Double anticipationHours, // horas de anticipacion o null

        @JsonProperty("port_timezone")
        String portTimezone, // zona IANA del puerto

        @JsonProperty("scheduled_departure")
        String scheduledDeparture, // zarpe programado en ISO_OFFSET_DATE_TIME

        @JsonProperty("cancelled_at")
        String cancelledAt, // instante de la solicitud en ISO_OFFSET_DATE_TIME

        @JsonProperty("reason")
        String reason, // motivo del propietario o null

        @JsonProperty("justification")
        String justification, // justificacion o null

        @JsonProperty("vessel_id")
        UUID vesselId, // embarcacion asociada

        @JsonProperty("vessel_new_operational_status")
        String vesselNewOperationalStatus, // estado operativo o null

        @JsonProperty("m1_sync_status")
        String m1SyncStatus, // estado del aviso a M1 o null

        @JsonProperty("m3_notification_status")
        String m3NotificationStatus, // estado del evento a M3 o null

        @JsonProperty("message")
        String message // resumen informativo
) {

    private static final String FLEXIBLE_MESSAGE = "Reserva cancelada con 72 horas o más de anticipación. Clasificación Flexible aplicada. El activo náutico ha quedado Disponible."; // texto flexible

    private static final String MODERATE_MESSAGE = "Reserva cancelada con entre 24 y 72 horas de anticipación. Clasificación Moderado aplicada. El activo náutico ha quedado Disponible."; // texto moderado

    private static final String LATE_MESSAGE = "Reserva cancelada con menos de 24 horas de anticipación. Clasificación Tardío aplicada. Módulo 3 tramitará la liquidación sin reembolso al arrendatario."; // texto tardio

    private static final String OWNER_MAINTENANCE_MESSAGE = "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. La embarcación ha sido transferida a 'En Mantenimiento/Limpieza' en Módulo 1 para inhabilitar nuevas reservas. Las reservas futuras confirmadas se mantienen intactas (sin cancelación en cascada)."; // texto con aviso de mantenimiento

    private static final String OWNER_MECHANICAL_MESSAGE = "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. Avería mecánica registrada."; // texto de averia sin aviso a M1

    private static final String OWNER_LOGISTICS_MESSAGE = "Cancelación por Propietario procesada. Clasificación Por Propietario asignada. La embarcación se mantiene Disponible al no mediar avería mecánica."; // texto de fuerza mayor

    private static final String DEFAULT_MESSAGE = "La reserva ha sido cancelada exitosamente bajo la política clasificada. Módulo 3 gestionará la liquidación financiera correspondiente."; // texto generico

    public static CancellationResponse from(CancellationResult result) { // construye el payload del contrato
        return new CancellationResponse(
                result.reservationId(), // reserva
                result.status().name(), // estado como texto
                result.cancellationSubtype().name(), // subtipo como texto
                result.requestingActor().name(), // actor como texto
                result.anticipationHours(), // horas o null
                result.portTimezone(), // zona del puerto
                format(result.scheduledDeparture()), // zarpe formateado
                format(result.cancelledAt()), // solicitud formateada
                result.reason(), // motivo
                result.justification(), // justificacion
                result.vesselId(), // embarcacion
                result.vesselNewOperationalStatus() == null ? null : result.vesselNewOperationalStatus().name(), // estado M1
                result.m1SyncStatus(), // aviso M1
                result.m3NotificationStatus(), // evento M3
                messageFor(result)); // mensaje segun subtipo
    }

    private static String format(OffsetDateTime value) { // formatea fechas con offset
        return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); // evita npe y conserva offset
    }

    private static String messageFor(CancellationResult result) { // selecciona el texto informativo
        return switch (result.cancellationSubtype()) {
            case FLEXIBLE -> FLEXIBLE_MESSAGE; // politica flexible
            case MODERATE -> MODERATE_MESSAGE; // politica moderada
            case LATE -> LATE_MESSAGE; // politica tardia
            case BY_OWNER -> ownerMessage(result); // propietario
            case NO_SHOW -> DEFAULT_MESSAGE; // no ocurre en CU-04
        };
    }

    private static String ownerMessage(CancellationResult result) { // diferencia los tres casos del propietario
        if ("mechanical_failure".equals(result.reason())) { // averia mecanica
            if (result.vesselNewOperationalStatus() == OperationalStatus.MAINTENANCE_CLEANING) { // hubo aviso a M1
                return OWNER_MAINTENANCE_MESSAGE; // transferencia a mantenimiento
            }
            if (result.vesselNewOperationalStatus() == null) { // no hubo aviso a M1
                return OWNER_MECHANICAL_MESSAGE; // solo se registra la averia
            }
        }
        if ("logistics_force_majeure".equals(result.reason())) { // fuerza mayor logistica
            return OWNER_LOGISTICS_MESSAGE; // se mantiene disponible
        }
        return DEFAULT_MESSAGE; // caso no previsto
    }
}