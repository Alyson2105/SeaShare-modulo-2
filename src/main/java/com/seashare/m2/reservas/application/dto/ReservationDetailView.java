package com.seashare.m2.reservas.application.dto;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationDetailView( // vista completa del detalle de CU-21

        UUID reservationId, // identificador de la reserva

        String reservationCode, // codigo visible persistido

        ReservationStatus status, // estado vigente

        CancellationSubtype cancellationSubtype, // subestado de cancelacion

        VesselDetail vessel, // ficha de la embarcacion; nunca null, con campos nulos si M1 falla

        Itinerary itinerary, // cronologia pactada y real

        Party renter, // arrendatario titular

        Party owner, // propietario anfitrion

        PaymentSummary paymentSummary, // desglose persistido adaptado al estado

        AvailableActions availableActions, // flags de acciones permitidas

        TimeIndicators timeIndicators, // contadores de tiempo

        Banner contextualBanner, // banner informativo o de alerta

        OffsetDateTime createdAt // creacion en la zona del puerto
) {

    public record Itinerary(
            OffsetDateTime scheduledDeparture, // zarpe pactado
            OffsetDateTime scheduledArrival, // llegada pactada (sin hora propia en M2)
            OffsetDateTime actualDepartureAt, // zarpe real; null hasta CU-06
            OffsetDateTime actualArrivalAt, // llegada real; null hasta CU-07
            DeparturePort departurePort, // puerto de zarpe
            int durationDays, // dias inclusivos
            int durationNights, // noches
            int passengers // ocupantes autorizados
    ) {
    }

    public record DeparturePort(
            String name, // nombre del puerto
            String city, // ciudad; no existe en el contrato de M1
            String timezone // zona IANA del puerto
    ) {
    }

    public record Party(
            UUID id, // identificador de la parte
            String name, // nombre disponible
            Long previousTrips // solo para el arrendatario; null para el propietario
    ) {
    }

    public record PaymentSummary(
            String sectionTitle, // titulo adaptado al estado
            String currency, // moneda persistida
            BigDecimal rentalAmount, // alquiler; null si no esta persistido o si el estado lo oculta
            BigDecimal insuranceAmount, // seguro; null si no esta persistido o si el estado lo oculta
            BigDecimal guaranteeDepositAmount, // deposito; null si no esta persistido o si el estado lo oculta
            BigDecimal estimatedTotal, // estimacion previa al pago; nunca se presenta como liquidada
            BigDecimal calculatedTotal, // importe solicitado para el cobro
            BigDecimal totalPaid, // total liquidado; null si aun no existe
            String clarifyingNote // nota cualitativa de liquidacion
    ) {
    }

    public record AvailableActions(
            boolean canCancel, // RESERVED
            boolean canInitiatePayment, // INITIATED y arrendatario
            boolean canMarkTripStart, // propietario, RESERVED y hora de zarpe cumplida
            boolean canMarkNoShow, // propietario, RESERVED y tolerancia de 30 min cumplida
            boolean canMarkTripEnd // propietario e IN_NAVIGATION
    ) {
    }

    public record TimeIndicators(
            Long ttlRemainingSeconds, // INITIATED/PENDING_PAYMENT
            Long minutesToDeparture, // RESERVED
            Long courtesyElapsedMinutes, // RESERVED despues de la hora pactada
            Long disputeWindowRemainingSeconds // COMPLETED, ventana de 24 h
    ) {
    }

    public record Banner(
            String type, // informative, active o alert
            String title, // titulo del banner
            String message // mensaje del banner
    ) {
    }
}
