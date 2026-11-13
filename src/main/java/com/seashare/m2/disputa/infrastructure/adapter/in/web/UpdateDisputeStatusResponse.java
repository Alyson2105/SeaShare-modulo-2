package com.seashare.m2.disputa.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusResult;
import com.seashare.m2.disputa.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

public record UpdateDisputeStatusResponse( // respuesta 200 de la resolucion administrativa (CU-17)

        @JsonProperty("dispute_id")
        UUID disputeId,

        @JsonProperty("reservation_id")
        UUID reservationId,

        @JsonProperty("previous_status")
        String previousStatus,

        @JsonProperty("new_status")
        String newStatus,

        @JsonProperty("resolved_by")
        UUID resolvedBy,

        @JsonProperty("resolved_at")
        Instant resolvedAt,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("m3_event_published")
        boolean m3EventPublished, // true = evento encolado en outbox para M3 (se publica de forma asincrona)

        @JsonProperty("message")
        String message
) {

    public static UpdateDisputeStatusResponse from(UpdateDisputeStatusResult result) { // proyecta al contrato
        return new UpdateDisputeStatusResponse(
                result.disputeId(),
                result.reservationId(),
                result.previousStatus().name(),
                result.newStatus().name(),
                result.resolvedBy(),
                result.resolvedAt(),
                result.reason(),
                result.m3EventPublished(),
                messageFor(result.newStatus()));
    }

    private static String messageFor(DisputeStatus status) { // resumen textual segun el destino
        return switch (status) {
            case ACCEPTED -> "Disputa resuelta como ACCEPTED. Modulo 3 ejecutara la liquidacion del deposito al Propietario.";
            case REJECTED -> "Disputa resuelta como REJECTED. Modulo 3 ejecutara el reembolso del deposito al Arrendatario.";
            case PENDING -> "Revision en curso registrada. La disputa permanece en PENDING. No se emitio ninguna notificacion a Modulo 3.";
        };
    }
}
