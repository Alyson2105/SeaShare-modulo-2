package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.ReservationListFilter;
import com.seashare.m2.reservas.application.dto.ReservationPage;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.VesselDetail;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationQueryServiceTest { // pruebas unitarias del filtrado, detalle y fail-safe de las vistas

    private static final Instant NOW = Instant.parse("2027-04-01T00:00:00Z"); // instante fijo de prueba

    private final ReservationRepositoryPort repository = mock(ReservationRepositoryPort.class); // puerto simulado

    private final VesselReadPort vesselReadPort = mock(VesselReadPort.class); // lectura de flota simulada

    private final ClockPort clockPort = mock(ClockPort.class); // reloj simulado

    private final ReservationQueryService service = new ReservationQueryService(repository, vesselReadPort, clockPort); // servicio bajo prueba

    private Reservation reservation(UUID renterId, UUID vesselId, ReservationStatus status) { // reserva minima para la proyeccion
        return Reservation.builder()
                .id(UUID.randomUUID())
                .renterId(renterId)
                .ownerId(UUID.randomUUID())
                .vesselId(vesselId)
                .startDate(LocalDate.of(2027, 4, 10))
                .endDate(LocalDate.of(2027, 4, 12))
                .passengers(2)
                .holderName("Juan Perez")
                .estimatedTotal(new BigDecimal("5000000.0000"))
                .currency("COP")
                .departureTime(LocalTime.of(8, 30))
                .harborTimezone("America/Bogota")
                .ttlExpiresAt(NOW.plusSeconds(900))
                .createdAt(NOW)
                .status(status)
                .version(0L)
                .build();
    }

    @Test
    void calculatedTotalIsNeverPresentedAsConfirmedWithoutEvidence() {
        for (ReservationStatus status : ReservationStatus.values()) {
            Reservation reservation = reservation(UUID.randomUUID(), UUID.randomUUID(), status);
            reservation.setTotalAmount(new BigDecimal("125.00")); // calculo simulado, guardado al iniciar pago
            var summary = ReservationDetailPolicy.paymentSummary(ReservationView.from(reservation));
            assertThat(summary.calculatedTotal()).as(status.name()).isEqualByComparingTo("125.00");
            assertThat(summary.totalPaid()).as(status.name()).isNull();
        }
    }

    @Test
    void historyFilterDelegatesWithHistoryStatuses() { // el filtro history se traduce al conjunto de estados del contrato
        UUID renter = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();
        when(repository.findByRenter(eq(renter), eq(1), eq(10), any()))
                .thenReturn(new ReservationPage(List.of(reservation(renter, vesselId, ReservationStatus.INITIATED)), 1));
        when(vesselReadPort.getSummaries(any()))
                .thenReturn(Map.of(vesselId, new VesselSummary(vesselId, "Catamaran", null, "Marina", null, "Dueno")));

        ReservationViewPage page = service.findReservations(renter, "RENTER", 1, 10, ReservationListFilter.HISTORY);

        assertThat(page.filter()).isEqualTo(ReservationListFilter.HISTORY);
        assertThat(page.role()).isEqualTo("RENTER");
        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.vessels()).containsKey(vesselId);
        verify(repository).findByRenter(eq(renter), eq(1), eq(10), eq(ReservationListFilter.HISTORY.statuses()));
    }

    @Test
    void cancelledFilterUsesOnlyTerminalFailureStatuses() { // cancelled agrupa CANCELLED, EXPIRED y PAYMENT_FAILED
        assertThat(ReservationListFilter.CANCELLED.statuses())
                .containsExactlyInAnyOrder(ReservationStatus.CANCELLED, ReservationStatus.EXPIRED, ReservationStatus.PAYMENT_FAILED);
    }

    @Test
    void ownerRoleQueriesByOwner() { // el propietario consulta por owner_id
        UUID owner = UUID.randomUUID();
        when(repository.findByOwner(eq(owner), eq(1), eq(10), any()))
                .thenReturn(new ReservationPage(List.of(), 0));

        ReservationViewPage page = service.findReservations(owner, "OWNER", 1, 10, ReservationListFilter.ALL);

        assertThat(page.role()).isEqualTo("OWNER");
        verify(repository).findByOwner(eq(owner), eq(1), eq(10), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void vesselFailureDoesNotBreakTheList() { // fail-safe por elemento: la lista responde aunque M1 falle
        UUID renter = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();
        when(repository.findByRenter(eq(renter), eq(1), eq(10), any()))
                .thenReturn(new ReservationPage(List.of(reservation(renter, vesselId, ReservationStatus.INITIATED)), 1));
        when(vesselReadPort.getSummaries(any())).thenThrow(new IllegalStateException("M1 caido"));

        ReservationViewPage page = service.findReservations(renter, "RENTER", 1, 10, ReservationListFilter.ALL);

        assertThat(page.items()).hasSize(1);
        assertThat(page.vessels()).isEmpty();
    }

    @Test
    void detailIncludesVesselAndPreviousTrips() { // el detalle enriquece con flota y conteo de viajes
        UUID renter = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();
        Reservation reservation = reservation(renter, vesselId, ReservationStatus.RESERVED);
        when(clockPort.now()).thenReturn(NOW);
        when(repository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(repository.countCompletedByRenterExcluding(renter, reservation.getId())).thenReturn(2L);
        when(vesselReadPort.getDetail(vesselId)).thenReturn(new VesselDetail(vesselId, "Catamaran", "REG-001", "CATAMARAN",
                "https://img", 12, 48.5, 3, true, "Marina", null, 11.0, -74.0,
                "America/Bogota", List.of("GPS"), UUID.randomUUID(), "Dueno", true));

        ReservationDetailView view = service.getReservationDetailView(reservation.getId(), renter, "RENTER");

        assertThat(view.vessel().name()).isEqualTo("Catamaran");
        assertThat(view.vessel().amenities()).containsExactly("GPS");
        assertThat(view.renter().previousTrips()).isEqualTo(2L);
        assertThat(view.owner().name()).isEqualTo("Dueno");
        assertThat(view.itinerary().durationDays()).isEqualTo(3);
        assertThat(view.itinerary().durationNights()).isEqualTo(2);
        assertThat(view.paymentSummary().totalPaid()).isNull(); // sin liquidacion no se usa estimated_total
        assertThat(view.availableActions().canCancel()).isTrue();
        assertThat(view.availableActions().canInitiatePayment()).isFalse();
    }

    @Test
    void detailFallsBackWhenFleetFails() { // M1 caido no rompe el detalle
        UUID renter = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();
        Reservation reservation = reservation(renter, vesselId, ReservationStatus.RESERVED);
        when(clockPort.now()).thenReturn(NOW);
        when(repository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(repository.countCompletedByRenterExcluding(renter, reservation.getId())).thenReturn(0L);
        when(vesselReadPort.getDetail(vesselId)).thenThrow(new IllegalStateException("M1 caido"));
        when(vesselReadPort.getSummaries(any())).thenThrow(new IllegalStateException("M1 caido"));

        ReservationDetailView view = service.getReservationDetailView(reservation.getId(), renter, "RENTER");

        assertThat(view.vessel().vesselId()).isEqualTo(vesselId);
        assertThat(view.vessel().name()).isNull();
        assertThat(view.owner().name()).isNull();
    }

    @Test
    void detailOfThirdPartyIsRejected() { // un tercero no accede al detalle
        UUID renter = UUID.randomUUID();
        Reservation reservation = reservation(renter, UUID.randomUUID(), ReservationStatus.RESERVED);
        when(repository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.getReservationDetailView(reservation.getId(), UUID.randomUUID(), "RENTER"))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.RESERVATION_NOT_OWNED.getCode()));
    }

    @Test
    void invalidPaginationIsRejected() { // page y limit se validan antes de consultar
        assertThatThrownBy(() -> service.findReservations(UUID.randomUUID(), "RENTER", 0, 10, ReservationListFilter.ALL))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETERS.getCode()));
        assertThatThrownBy(() -> service.findReservations(UUID.randomUUID(), "RENTER", 1, 51, ReservationListFilter.ALL))
                .isInstanceOf(ReservationRuleException.class);
    }

    @Test
    void invalidRoleIsRejected() { // solo RENTER y OWNER consultan
        assertThatThrownBy(() -> service.findReservations(UUID.randomUUID(), "ADMIN", 1, 10, ReservationListFilter.ALL))
                .isInstanceOfSatisfying(ReservationRuleException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.PROFILE_NOT_AUTHORIZED.getCode()));
    }
}
