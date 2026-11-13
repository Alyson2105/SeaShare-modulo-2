package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.contracts.dispute.OpenDisputeUseCase;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false")
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
class NavigationEndDisputeRollbackTest { // CU-07: si la disputa falla, el cierre revierte

    private static final Instant T0 = Instant.parse("2026-11-12T10:00:00Z"); // reloj controlado

    @Autowired
    private MockMvc mockMvc; // cliente HTTP

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // siembra la reserva

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // lleva a IN_NAVIGATION

    @Autowired
    private ReservationRepositoryPort reservationRepository; // lee el estado tras el rollback

    @Autowired
    private JdbcTemplate jdbcTemplate; // cuenta filas del evento tras el rollback

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // flota simulada

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // disponibilidad simulada

    @MockitoBean
    private ClockPort clockPort; // reloj controlado

    @MockitoBean
    private OpenDisputeUseCase openDisputeUseCase; // fallo controlado de la disputa

    private UUID ownerId; // propietario devuelto por M1

    @BeforeEach
    void setUp() { // prepara mocks de M1, reloj y fallo de disputa
        ownerId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(T0);
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
        when(openDisputeUseCase.open(any())).thenThrow(new IllegalStateException("dispute failure"));
    }

    private UUID createReservation() { // siembra una reserva INITIATED
        ReservationView view = createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12),
                2,
                "Juan Perez",
                "+573001234567",
                "juan@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("5000000.0000")
        ));
        return view.reservationId();
    }

    private void transition(UUID reservationId, ReservationStatus target, ActorType actor, UUID actorId) {
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, target, null, actor, "transicion de prueba",
                actorId, null, null, null, null, null));
    }

    private UUID inNavigation() { // siembra completa hasta IN_NAVIGATION
        UUID renterId = UUID.randomUUID();
        UUID reservationId = createReservation();
        transition(reservationId, ReservationStatus.PENDING_PAYMENT, ActorType.RENTER, renterId);
        transition(reservationId, ReservationStatus.RESERVED, ActorType.RENTER, renterId);
        transition(reservationId, ReservationStatus.IN_NAVIGATION, ActorType.OWNER, ownerId);
        return reservationId;
    }

    @Test
    void disputeFailureLeavesReservationInNavigation() throws Exception { // revierte el COMPLETED
        UUID reservationId = inNavigation();

        mockMvc.perform(post("/api/v1/reservations/{reservation_id}/navigation-end", reservationId)
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actual_arrival_at": "2027-04-10T17:30:00-05:00",
                                  "remarks": "Cierre con fallo de disputa"
                                }
                                """))
                .andExpect(status().isInternalServerError());

        Reservation saved = reservationRepository.findById(reservationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.IN_NAVIGATION);
        Integer checkoutRows = jdbcTemplate.queryForObject( // el evento de checkout no sobrevive al rollback
                "SELECT COUNT(*) FROM checkout_event WHERE reservation_id = ?", Integer.class, reservationId);
        assertThat(checkoutRows).isZero();
    }
}
