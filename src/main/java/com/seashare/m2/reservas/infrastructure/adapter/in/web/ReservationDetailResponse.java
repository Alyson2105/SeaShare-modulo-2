package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationDetailResponse( // contrato de respuesta del detalle de CU-21

        @JsonProperty("reservation_id")
        UUID reservationId, // identificador de la reserva

        @JsonProperty("reservation_code")
        String reservationCode, // codigo visible persistido

        @JsonProperty("status")
        ReservationStatus status, // estado vigente

        @JsonProperty("cancellation_subtype")
        CancellationSubtype cancellationSubtype, // subestado de cancelacion

        @JsonProperty("status_badge")
        StatusBadge statusBadge, // texto y color semantico del estado (CU-21 FR-015)

        @JsonProperty("vessel")
        VesselResponse vessel, // ficha de la embarcacion

        @JsonProperty("itinerary")
        ItineraryResponse itinerary, // cronologia pactada y real

        @JsonProperty("renter")
        RenterResponse renter, // arrendatario titular

        @JsonProperty("owner")
        OwnerResponse owner, // propietario anfitrion

        @JsonProperty("payment_summary")
        PaymentSummaryResponse paymentSummary, // desglose persistido adaptado

        @JsonProperty("available_actions")
        AvailableActionsResponse availableActions, // acciones permitidas

        @JsonProperty("time_indicators")
        TimeIndicatorsResponse timeIndicators, // contadores de tiempo

        @JsonProperty("contextual_banner")
        BannerResponse contextualBanner, // banner contextual

        @JsonProperty("created_at")
        OffsetDateTime createdAt // creacion en la zona del puerto
) {

    public record VesselResponse(
            @JsonProperty("vessel_id")
            UUID vesselId, // identificador de la embarcacion

            @JsonProperty("name")
            String name, // nombre comercial

            @JsonProperty("type")
            String type, // tipo de embarcacion

            @JsonProperty("image_url")
            String imageUrl, // imagen principal

            @JsonProperty("included_services")
            List<String> includedServices // comodidades incluidas
    ) {
    }

    public record ItineraryResponse(
            @JsonProperty("scheduled_departure")
            OffsetDateTime scheduledDeparture, // zarpe pactado

            @JsonProperty("scheduled_arrival")
            OffsetDateTime scheduledArrival, // llegada pactada

            @JsonProperty("actual_departure_at")
            OffsetDateTime actualDepartureAt, // zarpe real

            @JsonProperty("actual_arrival_at")
            OffsetDateTime actualArrivalAt, // llegada real

            @JsonProperty("departure_port")
            DeparturePortResponse departurePort, // puerto de zarpe

            @JsonProperty("duration_days")
            int durationDays, // dias inclusivos

            @JsonProperty("duration_nights")
            int durationNights, // noches

            @JsonProperty("passengers")
            int passengers // ocupantes autorizados
    ) {
    }

    public record DeparturePortResponse(
            @JsonProperty("name")
            String name, // nombre del puerto

            @JsonProperty("city")
            String city, // ciudad

            @JsonProperty("timezone")
            String timezone // zona IANA
    ) {
    }

    public record RenterResponse(
            @JsonProperty("renter_id")
            UUID renterId, // identificador del arrendatario

            @JsonProperty("name")
            String name, // nombre del titular

            @JsonProperty("previous_trips")
            Long previousTrips // viajes completados registrados en M2
    ) {
    }

    public record OwnerResponse(
            @JsonProperty("owner_id")
            UUID ownerId, // identificador del propietario

            @JsonProperty("name")
            String name // nombre validado
    ) {
    }

    public record PaymentSummaryResponse(
            @JsonProperty("section_title")
            String sectionTitle, // titulo adaptado al estado

            @JsonProperty("currency")
            String currency, // moneda persistida

            @JsonProperty("rental_amount")
            BigDecimal rentalAmount, // alquiler persistido

            @JsonProperty("insurance_amount")
            BigDecimal insuranceAmount, // seguro persistido

            @JsonProperty("guarantee_deposit_amount")
            BigDecimal guaranteeDepositAmount, // deposito persistido

            @JsonProperty("total_paid")
            BigDecimal totalPaid, // total liquidado o estimado

            @JsonProperty("clarifying_note")
            String clarifyingNote // nota de liquidacion
    ) {
    }

    public record AvailableActionsResponse(
            @JsonProperty("can_cancel")
            boolean canCancel, // cancelacion disponible

            @JsonProperty("can_initiate_payment")
            boolean canInitiatePayment, // inicio de pago disponible

            @JsonProperty("can_mark_trip_start")
            boolean canMarkTripStart, // inicio de navegacion disponible

            @JsonProperty("can_mark_no_show")
            boolean canMarkNoShow, // inasistencia disponible

            @JsonProperty("can_mark_trip_end")
            boolean canMarkTripEnd // fin de navegacion disponible
    ) {
    }

    public record TimeIndicatorsResponse(
            @JsonProperty("ttl_remaining_seconds")
            Long ttlRemainingSeconds, // segundos de TTL

            @JsonProperty("minutes_to_departure")
            Long minutesToDeparture, // minutos al zarpe

            @JsonProperty("courtesy_elapsed_minutes")
            Long courtesyElapsedMinutes, // minutos de cortesia

            @JsonProperty("dispute_window_remaining_seconds")
            Long disputeWindowRemainingSeconds // segundos de disputa
    ) {
    }

    public record BannerResponse(
            @JsonProperty("type")
            String type, // informative, active o alert

            @JsonProperty("title")
            String title, // titulo

            @JsonProperty("message")
            String message // mensaje
    ) {
    }

    public static ReservationDetailResponse from(ReservationDetailView view) { // transforma la vista de aplicacion
        VesselDetail vessel = view.vessel(); // ficha de embarcacion
        return new ReservationDetailResponse(
                view.reservationId(),
                view.reservationCode(),
                view.status(),
                view.cancellationSubtype(),
                StatusBadge.from(view.status(), view.cancellationSubtype()),
                new VesselResponse(vessel.vesselId(), vessel.name(), vessel.type(), vessel.imageUrl(),
                        vessel.amenities() == null ? List.of() : vessel.amenities()),
                new ItineraryResponse(
                        view.itinerary().scheduledDeparture(),
                        view.itinerary().scheduledArrival(),
                        view.itinerary().actualDepartureAt(),
                        view.itinerary().actualArrivalAt(),
                        new DeparturePortResponse(
                                view.itinerary().departurePort().name(),
                                view.itinerary().departurePort().city(),
                                view.itinerary().departurePort().timezone()),
                        view.itinerary().durationDays(),
                        view.itinerary().durationNights(),
                        view.itinerary().passengers()),
                new RenterResponse(view.renter().id(), view.renter().name(), view.renter().previousTrips()),
                new OwnerResponse(view.owner().id(), view.owner().name()),
                new PaymentSummaryResponse(
                        view.paymentSummary().sectionTitle(),
                        view.paymentSummary().currency(),
                        view.paymentSummary().rentalAmount(),
                        view.paymentSummary().insuranceAmount(),
                        view.paymentSummary().guaranteeDepositAmount(),
                        view.paymentSummary().totalPaid(),
                        view.paymentSummary().clarifyingNote()),
                new AvailableActionsResponse(
                        view.availableActions().canCancel(),
                        view.availableActions().canInitiatePayment(),
                        view.availableActions().canMarkTripStart(),
                        view.availableActions().canMarkNoShow(),
                        view.availableActions().canMarkTripEnd()),
                new TimeIndicatorsResponse(
                        view.timeIndicators().ttlRemainingSeconds(),
                        view.timeIndicators().minutesToDeparture(),
                        view.timeIndicators().courtesyElapsedMinutes(),
                        view.timeIndicators().disputeWindowRemainingSeconds()),
                new BannerResponse(
                        view.contextualBanner().type(),
                        view.contextualBanner().title(),
                        view.contextualBanner().message()),
                view.createdAt());
    }
}
