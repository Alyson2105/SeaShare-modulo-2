package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public record ReservationListItemResponse( // tarjeta de reserva del listado de CU-20

        @JsonProperty("reservation_id")
        UUID reservationId, // identificador de la reserva

        @JsonProperty("reservation_code")
        String reservationCode, // codigo visible persistido

        @JsonProperty("status")
        ReservationStatus status, // estado vigente

        @JsonProperty("cancellation_subtype")
        CancellationSubtype cancellationSubtype, // subestado de cancelacion, si aplica

        @JsonProperty("vessel")
        VesselResponse vessel, // datos referenciales de la embarcacion

        @JsonProperty("dates")
        DatesResponse dates, // rango temporal y duracion

        @JsonProperty("passengers")
        int passengers, // ocupantes autorizados

        @JsonProperty("total_price")
        TotalPriceResponse totalPrice, // monto original congelado

        @JsonProperty("renter")
        RenterResponse renter, // arrendatario titular

        @JsonProperty("owner")
        OwnerResponse owner, // propietario anfitrion

        @JsonProperty("contextual_label")
        String contextualLabel, // etiqueta cualitativa para el arrendatario

        @JsonProperty("detail_url")
        String detailUrl, // ruta del detalle (CU-21)

        @JsonProperty("created_at")
        OffsetDateTime createdAt // marca de creacion en la zona del puerto
) {

    public record VesselResponse(
            @JsonProperty("vessel_id")
            UUID vesselId, // identificador de la embarcacion

            @JsonProperty("name")
            String name, // nombre comercial

            @JsonProperty("image_url")
            String imageUrl, // imagen principal

            @JsonProperty("port")
            String port // puerto de salida
    ) {
    }

    public record DatesResponse(
            @JsonProperty("start_at")
            OffsetDateTime startAt, // inicio pactado

            @JsonProperty("end_at")
            OffsetDateTime endAt, // fin pactado

            @JsonProperty("duration_days")
            int durationDays, // dias inclusivos

            @JsonProperty("duration_nights")
            int durationNights // noches del rango
    ) {
    }

    public record TotalPriceResponse(
            @JsonProperty("amount")
            BigDecimal amount, // monto persistido literal

            @JsonProperty("currency")
            String currency, // moneda registrada

            @JsonProperty("is_frozen")
            boolean frozen // el monto es el original congelado
    ) {
    }

    public record RenterResponse(
            @JsonProperty("renter_id")
            UUID renterId, // identificador del arrendatario

            @JsonProperty("name")
            String name // nombre del titular
    ) {
    }

    public record OwnerResponse(
            @JsonProperty("owner_id")
            UUID ownerId, // identificador del propietario

            @JsonProperty("name")
            String name // nombre validado del propietario
    ) {
    }

    public static ReservationListItemResponse from(ReservationView view, VesselSummary summary, String role) { // arma la tarjeta
        ZoneId zone = ReservationTimeMapper.zoneOf(view.harborTimezone()); // zona del puerto
        VesselResponse vessel = new VesselResponse( // datos de embarcacion con fail-safe
                view.vesselId(),
                summary == null ? null : summary.name(),
                summary == null ? null : summary.imageUrl(),
                summary == null ? null : summary.portName());
        RenterResponse renter = new RenterResponse(view.renterId(), view.holderName()); // nombre del titular persistido
        OwnerResponse owner = new OwnerResponse(view.ownerId(), summary == null ? null : summary.ownerName()); // nombre desde M1
        return new ReservationListItemResponse(
                view.reservationId(),
                view.reservationCode(),
                view.status(),
                view.cancellationSubtype(),
                vessel,
                new DatesResponse(
                        ReservationTimeMapper.at(view.startDate(), view.departureTime(), zone),
                        ReservationTimeMapper.at(view.endDate(), view.departureTime(), zone),
                        ReservationTimeMapper.durationDays(view.startDate(), view.endDate()),
                        ReservationTimeMapper.durationNights(view.startDate(), view.endDate())),
                view.passengers(),
                new TotalPriceResponse(view.estimatedTotal(), view.currency(), true),
                renter,
                owner,
                contextualLabel(role, view.status(), view.cancellationSubtype()),
                "/api/v1/reservations/" + view.reservationId(),
                view.createdAt().atZone(zone).toOffsetDateTime());
    }

    static String contextualLabel(String role, ReservationStatus status, CancellationSubtype subtype) { // etiqueta cualitativa sin montos
        if (!"RENTER".equalsIgnoreCase(role)) { // la etiqueta de reembolso es para el arrendatario
            return null; // el propietario no la recibe
        }
        if (status == ReservationStatus.COMPLETED) { // viaje finalizado: liquidacion delegada
            return "Reembolso gestionado por Módulo 3"; // sin afirmar estado de disputa
        }
        if (status != ReservationStatus.CANCELLED) { // solo aplica a canceladas y completadas
            return null; // resto de estados sin etiqueta
        }
        if (subtype == null) { // cancelacion sin subestado
            return "Cancelada / Reembolso gestionado por Módulo 3"; // etiqueta generica
        }
        return switch (subtype) { // textos por subestado contractual
            case FLEXIBLE -> "Cancelada - Ventana Flexible / Reembolso gestionado por Módulo 3";
            case MODERATE -> "Cancelada - Ventana Moderada / Reembolso gestionado por Módulo 3";
            case LATE -> "Cancelada - Ventana Tardía / Reembolso gestionado por Módulo 3";
            case BY_OWNER -> "Cancelada - Por Propietario / Reembolso gestionado por Módulo 3";
            case NO_SHOW -> "Cancelada - Por Inasistencia / Reembolso gestionado por Módulo 3";
        };
    }
}
