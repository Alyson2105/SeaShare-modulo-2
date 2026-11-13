package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationDetailPolicyTest { // reglas de presentacion del detalle sin calculos monetarios

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota"); // zona del puerto de prueba

    private static final Instant NOW = Instant.parse("2027-04-10T12:00:00Z"); // instante fijo (07:00 en Bogota)

    private Reservation reservation(ReservationStatus status, CancellationSubtype subtype) { // reserva con datos completos
        return Reservation.builder()
                .id(UUID.randomUUID())
                .renterId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .vesselId(UUID.randomUUID())
                .startDate(LocalDate.of(2027, 4, 10))
                .endDate(LocalDate.of(2027, 4, 12))
                .passengers(2)
                .holderName("Juan Perez")
                .estimatedTotal(new BigDecimal("3000000.00"))
                .currency("COP")
                .departureTime(LocalTime.of(8, 30))
                .harborTimezone("America/Bogota")
                .ttlExpiresAt(NOW.plusSeconds(900))
                .createdAt(NOW)
                .status(status)
                .cancellationSubtype(subtype)
                .version(0L)
                .build();
    }

    private ReservationView view(Reservation reservation) { // proyecta la reserva
        return ReservationView.from(reservation);
    }

    private OffsetDateTime departure(ReservationView view) { // zarpe pactado en la zona del puerto
        return HarborTime.at(view.startDate(), view.departureTime(), BOGOTA);
    }

    @Test
    void reservedShowsBreakdownAndUsesLiquidatedTotal() { // RESERVED conserva los tres rubros persistidos
        Reservation reservation = reservation(ReservationStatus.RESERVED, null);
        reservation.setRentalAmount(new BigDecimal("2800000.00"));
        reservation.setInsuranceAmount(new BigDecimal("350000.00"));
        reservation.setGuaranteeDepositAmount(new BigDecimal("300000.00"));
        reservation.setTotalAmount(new BigDecimal("3450000.00"));

        ReservationDetailView.PaymentSummary summary = ReservationDetailPolicy.paymentSummary(view(reservation));

        assertThat(summary.sectionTitle()).isEqualTo("Resumen de pago");
        assertThat(summary.rentalAmount()).isEqualByComparingTo("2800000.00");
        assertThat(summary.insuranceAmount()).isEqualByComparingTo("350000.00");
        assertThat(summary.guaranteeDepositAmount()).isEqualByComparingTo("300000.00");
        assertThat(summary.totalPaid()).isEqualByComparingTo("3450000.00");
        assertThat(summary.clarifyingNote()).isNull();
    }

    @Test
    void completedHidesRentalAndInsurance() { // COMPLETED enfoca el deposito de garantia
        Reservation reservation = reservation(ReservationStatus.COMPLETED, null);
        reservation.setRentalAmount(new BigDecimal("2800000.00"));
        reservation.setInsuranceAmount(new BigDecimal("350000.00"));
        reservation.setGuaranteeDepositAmount(new BigDecimal("300000.00"));

        ReservationDetailView.PaymentSummary summary = ReservationDetailPolicy.paymentSummary(view(reservation));

        assertThat(summary.sectionTitle()).isEqualTo("Garantía en custodia");
        assertThat(summary.rentalAmount()).isNull();
        assertThat(summary.insuranceAmount()).isNull();
        assertThat(summary.guaranteeDepositAmount()).isEqualByComparingTo("300000.00");
        assertThat(summary.totalPaid()).isNull(); // sin monto liquidado no se presenta el estimado
        assertThat(summary.clarifyingNote()).isNotBlank();
    }

    @Test
    void totalPaidIsNullWhenTotalAmountIsMissing() { // sin liquidacion no hay total pagado
        Reservation reservation = reservation(ReservationStatus.RESERVED, null);
        reservation.setRentalAmount(new BigDecimal("2800000.00"));
        reservation.setInsuranceAmount(new BigDecimal("350000.00"));
        reservation.setGuaranteeDepositAmount(new BigDecimal("300000.00"));

        ReservationDetailView.PaymentSummary summary = ReservationDetailPolicy.paymentSummary(view(reservation));

        assertThat(summary.totalPaid()).isNull();
    }

    @Test
    void noShowUsesCompensationAndHidesBreakdown() { // NO_SHOW titula Compensacion y oculta rubros
        Reservation reservation = reservation(ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW);
        reservation.setRentalAmount(new BigDecimal("2800000.00"));
        reservation.setGuaranteeDepositAmount(new BigDecimal("300000.00"));

        ReservationDetailView.PaymentSummary summary = ReservationDetailPolicy.paymentSummary(view(reservation));

        assertThat(summary.sectionTitle()).isEqualTo("Compensación");
        assertThat(summary.rentalAmount()).isNull();
        assertThat(summary.insuranceAmount()).isNull();
        assertThat(summary.guaranteeDepositAmount()).isNull();
        assertThat(summary.clarifyingNote()).isEqualTo("El sistema de pagos gestionará la compensación al propietario.");
    }

    @Test
    void actionsDependOnRoleAndClock() { // flags de muelle por rol y hora
        ReservationView view = view(reservation(ReservationStatus.RESERVED, null));
        OffsetDateTime departure = departure(view);

        ReservationDetailView.AvailableActions renterBefore =
                ReservationDetailPolicy.actions(view, "RENTER", departure, NOW);
        assertThat(renterBefore.canCancel()).isTrue();
        assertThat(renterBefore.canMarkTripStart()).isFalse();

        ReservationDetailView.AvailableActions ownerBefore =
                ReservationDetailPolicy.actions(view, "OWNER", departure, NOW);
        assertThat(ownerBefore.canMarkTripStart()).isFalse();
        assertThat(ownerBefore.canMarkNoShow()).isFalse();

        Instant afterTolerance = departure.toInstant().plusSeconds(31 * 60);
        ReservationDetailView.AvailableActions ownerAfter =
                ReservationDetailPolicy.actions(view, "OWNER", departure, afterTolerance);
        assertThat(ownerAfter.canMarkTripStart()).isTrue();
        assertThat(ownerAfter.canMarkNoShow()).isTrue();
        assertThat(ownerAfter.canCancel()).isTrue();

        ReservationView navigating = view(reservation(ReservationStatus.IN_NAVIGATION, null));
        ReservationDetailView.AvailableActions ownerNavigating =
                ReservationDetailPolicy.actions(navigating, "OWNER", departure(navigating), NOW);
        assertThat(ownerNavigating.canMarkTripEnd()).isTrue();
        assertThat(ownerNavigating.canCancel()).isFalse();
    }

    @Test
    void timeIndicatorsComputeCounters() { // contadores de TTL, zarpe, cortesia y disputa
        Reservation initiated = reservation(ReservationStatus.INITIATED, null);
        ReservationDetailView.TimeIndicators ttl =
                ReservationDetailPolicy.timeIndicators(view(initiated), departure(view(initiated)), NOW);
        assertThat(ttl.ttlRemainingSeconds()).isEqualTo(900L);
        assertThat(ttl.minutesToDeparture()).isNull();

        ReservationView reserved = view(reservation(ReservationStatus.RESERVED, null));
        OffsetDateTime departure = departure(reserved);
        ReservationDetailView.TimeIndicators before =
                ReservationDetailPolicy.timeIndicators(reserved, departure, departure.toInstant().minusSeconds(90));
        assertThat(before.minutesToDeparture()).isEqualTo(1L);
        assertThat(before.courtesyElapsedMinutes()).isNull();

        ReservationDetailView.TimeIndicators after =
                ReservationDetailPolicy.timeIndicators(reserved, departure, departure.toInstant().plusSeconds(300));
        assertThat(after.courtesyElapsedMinutes()).isEqualTo(5L);

        Reservation completed = reservation(ReservationStatus.COMPLETED, null);
        completed.setActualArrivalAt(NOW.minusSeconds(3600));
        completed.setStatusChangedAt(NOW.minusSeconds(3600));
        ReservationView completedView = view(completed);
        ReservationDetailView.TimeIndicators dispute =
                ReservationDetailPolicy.timeIndicators(completedView, departure(completedView), NOW);
        assertThat(dispute.disputeWindowRemainingSeconds()).isEqualTo(82800L);
    }

    @Test
    void bannerReflectsState() { // banner informativo vs alerta terminal
        ReservationView reserved = view(reservation(ReservationStatus.RESERVED, null));
        assertThat(ReservationDetailPolicy.banner(reserved, null).type()).isEqualTo("informative");

        Reservation canceled = reservation(ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW);
        ReservationDetailView.Banner banner = ReservationDetailPolicy.banner(view(canceled), null);
        assertThat(banner.type()).isEqualTo("alert");
        assertThat(banner.title()).isEqualTo("Reserva cancelada por inasistencia");
    }
}
