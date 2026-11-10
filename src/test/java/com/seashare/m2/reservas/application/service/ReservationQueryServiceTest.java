package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationListFilter;
import com.seashare.m2.reservas.application.dto.ReservationPage;
import com.seashare.m2.reservas.application.dto.ReservationViewPage;
import com.seashare.m2.reservas.application.dto.VesselSummary;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.VesselReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationQueryServiceTest { // pruebas unitarias del filtrado y del fail-safe de la vista de listado

    private final ReservationRepositoryPort repository = mock(ReservationRepositoryPort.class); // puerto simulado

    private final VesselReadPort vesselReadPort = mock(VesselReadPort.class); // lectura de flota simulada

    private final ReservationQueryService service = new ReservationQueryService(repository, vesselReadPort); // servicio bajo prueba

    private Reservation reservation(UUID renterId, UUID vesselId) { // reserva minima para la proyeccion
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
                .ttlExpiresAt(Instant.parse("2027-04-01T00:15:00Z"))
                .createdAt(Instant.parse("2027-04-01T00:00:00Z"))
                .status(ReservationStatus.INITIATED)
                .version(0L)
                .build();
    }

    @Test
    void historyFilterDelegatesWithHistoryStatuses() { // el filtro history se traduce al conjunto de estados del contrato
        UUID renter = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();
        when(repository.findByRenter(eq(renter), eq(1), eq(10), any()))
                .thenReturn(new ReservationPage(List.of(reservation(renter, vesselId)), 1));
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
                .thenReturn(new ReservationPage(List.of(reservation(renter, vesselId)), 1));
        when(vesselReadPort.getSummaries(any())).thenThrow(new IllegalStateException("M1 caido"));

        ReservationViewPage page = service.findReservations(renter, "RENTER", 1, 10, ReservationListFilter.ALL);

        assertThat(page.items()).hasSize(1);
        assertThat(page.vessels()).isEmpty();
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
