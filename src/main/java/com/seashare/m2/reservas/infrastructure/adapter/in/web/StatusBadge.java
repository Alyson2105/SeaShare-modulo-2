package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

/**
 * Metadatos visuales del estado de una reserva (CU-20 FR-008 y CU-21 FR-015).
 *
 * <p>Clase de presentacion pura: traduce el estado interno y, si aplica, el subtipo de cancelacion
 * a un texto legible, un color semantico y una descripcion. No contiene logica de negocio ni montos.</p>
 */
public record StatusBadge(

        @JsonProperty("text")
        String text, // etiqueta legible para el usuario

        @JsonProperty("semantic_color")
        String semanticColor, // verde, amarillo, gris o rojo

        @JsonProperty("description")
        String description // aclaracion breve del estado
) {

    static final String GREEN = "verde"; // estado estable y confirmado

    static final String YELLOW = "amarillo"; // estado en curso o pendiente de accion

    static final String GRAY = "gris"; // estado cerrado sin conflicto o informativo

    static final String RED = "rojo"; // estado con perdida o fallo

    public static StatusBadge from(ReservationStatus status, CancellationSubtype subtype) { // mapea estado y subtipo
        return switch (status) {
            case INITIATED -> new StatusBadge("Iniciada", GRAY,
                    "Reserva creada; el pago está pendiente"); // embudo de pago
            case PENDING_PAYMENT -> new StatusBadge("Pago pendiente", YELLOW,
                    "Estamos confirmando el pago de la reserva"); // pago en curso
            case RESERVED -> new StatusBadge("Confirmada", GREEN,
                    "Pago confirmado y lista para zarpe"); // plan CU-20/CU-21
            case IN_NAVIGATION -> new StatusBadge("En navegación", YELLOW,
                    "Viaje náutico en curso"); // plan CU-20
            case COMPLETED -> new StatusBadge("Completada", GRAY,
                    "Viaje finalizado exitosamente"); // plan CU-20
            case EXPIRED -> new StatusBadge("Expirada", GRAY,
                    "El tiempo para completar el pago venció"); // TTL vencido
            case PAYMENT_FAILED -> new StatusBadge("Pago fallido", RED,
                    "No se pudo confirmar el pago de la reserva"); // pago rechazado
            case CANCELLED -> cancelled(subtype); // el texto depende del subtipo
        };
    }

    private static StatusBadge cancelled(CancellationSubtype subtype) { // subestados de cancelacion
        if (subtype == null) { // cancelacion sin subestado registrado
            return new StatusBadge("Cancelada", RED, "La reserva fue cancelada"); // etiqueta generica
        }
        return switch (subtype) {
            case FLEXIBLE -> new StatusBadge("Cancelada", RED,
                    "Cancelada voluntariamente con anticipación > 72 h"); // plan CU-20
            case MODERATE -> new StatusBadge("Cancelada", RED,
                    "Cancelada voluntariamente con anticipación entre 72 y 24 h"); // ventana moderada
            case LATE -> new StatusBadge("Cancelada", RED,
                    "Cancelada voluntariamente con anticipación < 24 h"); // ventana tardia
            case BY_OWNER -> new StatusBadge("Cancelada por el propietario", RED,
                    "El propietario canceló la reserva"); // decision del anfitrion
            case NO_SHOW -> new StatusBadge("Cancelado por inasistencia", RED,
                    "El arrendatario no se presentó tras cumplirse la cortesía de 30 minutos"); // plan CU-20
        };
    }
}
