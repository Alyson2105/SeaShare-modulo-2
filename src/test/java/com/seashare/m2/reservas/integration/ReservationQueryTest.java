package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(PostgresTestContainers.class)
@AutoConfigureMockMvc
@Transactional
class ReservationQueryTest { // pruebas de integracion de endpoints de consulta usando MockMvc y los casos de uso

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para realizar peticiones rest

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso para sembrar reservas

    @Autowired
    private ChangeReservationStatusUseCase changeReservationStatusUseCase; // caso de uso para sembrar estados

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota requerida por el contexto

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad operativa

    private UUID ownerId; // propietario devuelto por el mock de flota

    @BeforeEach
    void setUp() { // configuracion inicial de mocks de flota
        ownerId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true); // el resumen de flota lo aporta el adaptador fake por defecto
    }

    private ReservationView createReservation(UUID renterId, UUID vesselId) { // siembra una reserva INITIATED
        return createReservationUseCase.execute(new CreateReservationCommand(
                renterId,
                UUID.randomUUID().toString(),
                vesselId,
                LocalDate.of(2027, 4, 10),
                LocalDate.of(2027, 4, 12),
                2,
                "Juan Perez",
                "+573001234567",
                "juan@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("5000000.0000")
        ));
    }

    private void toPendingPayment(UUID reservationId, UUID renterId) { // transicion valida INITIATED -> PENDING_PAYMENT
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                renterId, null, null, null, null, null));
    }

    private void toReserved(UUID reservationId, UUID renterId) { // transicion valida PENDING_PAYMENT -> RESERVED
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.RESERVED, null, ActorType.RENTER, "Pago confirmado",
                renterId, null, null, null, null, null));
    }

    private void toCancelled(UUID reservationId, UUID renterId) { // transicion valida RESERVED -> CANCELLED FLEXIBLE
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE, ActorType.RENTER,
                "Cancelacion voluntaria", renterId, null, null, 5000, null, null));
    }

    private void toInNavigation(UUID reservationId, UUID ownerId) { // transicion valida RESERVED -> IN_NAVIGATION
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.IN_NAVIGATION, null, ActorType.OWNER,
                "Inicio de navegacion", ownerId, null, null, null, null, null));
    }

    private void toCompleted(UUID reservationId, UUID ownerId) { // transicion valida IN_NAVIGATION -> COMPLETED
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.COMPLETED, null, ActorType.OWNER,
                "Fin de navegacion", ownerId, null, null, null, "Viaje completado", null));
    }

    private void toNoShow(UUID reservationId, UUID ownerId) { // transicion valida RESERVED -> CANCELLED NO_SHOW
        changeReservationStatusUseCase.execute(new ChangeReservationStatusCommand(
                reservationId, ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW, ActorType.OWNER,
                "Inasistencia", ownerId, null, false, null, null, null));
    }

    private ReservationView completedReservation(UUID renterId) { // siembra una reserva COMPLETED
        ReservationView created = createReservation(renterId, UUID.randomUUID());
        toPendingPayment(created.reservationId(), renterId);
        toReserved(created.reservationId(), renterId);
        toInNavigation(created.reservationId(), ownerId);
        toCompleted(created.reservationId(), ownerId);
        return created;
    }

    @Test
    void listsOnlyOwnReservations() throws Exception { // consulta paginada retorna exclusivamente reservas propias del llamador
        UUID userRenter = UUID.randomUUID();
        UUID otherRenter = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();

        createReservation(userRenter, vesselId);
        createReservation(userRenter, vesselId);
        createReservation(otherRenter, vesselId);

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", userRenter.toString())
                        .header("X-User-Role", "RENTER")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.page_size").value(10))
                .andExpect(jsonPath("$.pagination.total_items").value(2))
                .andExpect(jsonPath("$.pagination.total_pages").value(1))
                .andExpect(jsonPath("$.applied_filter").value("all"))
                .andExpect(jsonPath("$.user_role").value("RENTER"))
                .andExpect(jsonPath("$.view_title").value("Mis reservas"))
                .andExpect(jsonPath("$.reservations.length()").value(2))
                .andExpect(jsonPath("$.reservations[0].reservation_id").isNotEmpty())
                .andExpect(jsonPath("$.reservations[0].reservation_code").isNotEmpty())
                .andExpect(jsonPath("$.reservations[0].vessel.name").value(org.hamcrest.Matchers.startsWith("Embarcacion demo")))
                .andExpect(jsonPath("$.reservations[0].vessel.port").value("Marina demo"))
                .andExpect(jsonPath("$.reservations[0].renter.name").value("Juan Perez"))
                .andExpect(jsonPath("$.reservations[0].owner.name").value("Propietario demo"))
                .andExpect(jsonPath("$.reservations[0].total_price.amount").isNumber())
                .andExpect(jsonPath("$.reservations[0].total_price.currency").value("COP"))
                .andExpect(jsonPath("$.reservations[0].total_price.is_frozen").value(true))
                .andExpect(jsonPath("$.reservations[0].dates.duration_days").value(3))
                .andExpect(jsonPath("$.reservations[0].dates.duration_nights").value(2))
                .andExpect(jsonPath("$.reservations[0].detail_url").isNotEmpty())
                .andExpect(jsonPath("$.reservations[0].status_badge").doesNotExist());

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(3))
                .andExpect(jsonPath("$.user_role").value("OWNER"))
                .andExpect(jsonPath("$.view_title").value("Reservas recibidas"))
                .andExpect(jsonPath("$.reservations.length()").value(3));
    }

    @Test
    void listsWithAppliedFilter() throws Exception { // history excluye canceladas y cancelled las aisla
        UUID renter = UUID.randomUUID();

        ReservationView reserved = createReservation(renter, UUID.randomUUID()); // embarcacion distinta evita solapamiento
        toPendingPayment(reserved.reservationId(), renter);
        toReserved(reserved.reservationId(), renter);

        ReservationView cancelled = createReservation(renter, UUID.randomUUID()); // embarcacion distinta evita solapamiento
        toPendingPayment(cancelled.reservationId(), renter);
        toReserved(cancelled.reservationId(), renter);
        toCancelled(cancelled.reservationId(), renter);

        createReservation(renter, UUID.randomUUID()); // queda INITIATED y pertenece al historial

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", renter.toString())
                        .header("X-User-Role", "RENTER")
                        .param("filter", "history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied_filter").value("history"))
                .andExpect(jsonPath("$.pagination.total_items").value(2))
                .andExpect(jsonPath("$.reservations.length()").value(2));

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", renter.toString())
                        .header("X-User-Role", "RENTER")
                        .param("filter", "cancelled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied_filter").value("cancelled"))
                .andExpect(jsonPath("$.pagination.total_items").value(1))
                .andExpect(jsonPath("$.reservations.length()").value(1))
                .andExpect(jsonPath("$.reservations[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.reservations[0].cancellation_subtype").value("FLEXIBLE"))
                .andExpect(jsonPath("$.reservations[0].contextual_label")
                        .value("Cancelada - Ventana Flexible / Reembolso gestionado por Módulo 3"));

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", renter.toString())
                        .header("X-User-Role", "RENTER")
                        .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(3));
    }

    @Test
    void emptyStateHasZeroTotals() throws Exception { // usuario sin reservas recibe arreglo vacio y metadatos en cero
        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .param("filter", "cancelled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.total_items").value(0))
                .andExpect(jsonPath("$.pagination.total_pages").value(0))
                .andExpect(jsonPath("$.reservations.length()").value(0));
    }

    @Test
    void invalidFilterIs400() throws Exception { // filtro no soportado retorna 400 INVALID_PARAMETERS
        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER")
                        .param("filter", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void detailIncludesContractFields() throws Exception { // detalle enriquecido de CU-21 segun el contrato
        UUID renter = UUID.randomUUID();
        ReservationView created = createReservation(renter, UUID.randomUUID());
        toPendingPayment(created.reservationId(), renter);
        toReserved(created.reservationId(), renter);

        mockMvc.perform(get("/api/v1/reservations/" + created.reservationId())
                        .header("X-User-Id", renter.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation_id").value(created.reservationId().toString()))
                .andExpect(jsonPath("$.reservation_code").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.vessel.vessel_id").isNotEmpty())
                .andExpect(jsonPath("$.vessel.name").value(org.hamcrest.Matchers.startsWith("Embarcacion demo")))
                .andExpect(jsonPath("$.vessel.type").value("CATAMARAN"))
                .andExpect(jsonPath("$.vessel.included_services[0]").value("GPS Nautico"))
                .andExpect(jsonPath("$.itinerary.scheduled_departure").isNotEmpty())
                .andExpect(jsonPath("$.itinerary.departure_port.timezone").value("America/Bogota"))
                .andExpect(jsonPath("$.itinerary.duration_days").value(3))
                .andExpect(jsonPath("$.itinerary.duration_nights").value(2))
                .andExpect(jsonPath("$.itinerary.passengers").value(2))
                .andExpect(jsonPath("$.renter.name").value("Juan Perez"))
                .andExpect(jsonPath("$.renter.previous_trips").value(0))
                .andExpect(jsonPath("$.owner.name").value("Propietario demo"))
                .andExpect(jsonPath("$.payment_summary.section_title").value("Resumen de pago"))
                .andExpect(jsonPath("$.payment_summary.currency").value("COP"))
                .andExpect(jsonPath("$.payment_summary.total_paid").isNumber())
                .andExpect(jsonPath("$.payment_summary.rental_amount").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.payment_summary.insurance_amount").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.available_actions.can_cancel").value(true))
                .andExpect(jsonPath("$.available_actions.can_initiate_payment").value(false))
                .andExpect(jsonPath("$.contextual_banner.title").value("Reserva confirmada"))
                .andExpect(jsonPath("$.status_badge").doesNotExist());
    }

    @Test
    void detailOfNoShowUsesCompensation() throws Exception { // NO_SHOW cambia el bloque a Compensacion (FR-014)
        UUID renter = UUID.randomUUID();
        ReservationView created = createReservation(renter, UUID.randomUUID());
        toPendingPayment(created.reservationId(), renter);
        toReserved(created.reservationId(), renter);
        toNoShow(created.reservationId(), ownerId);

        mockMvc.perform(get("/api/v1/reservations/" + created.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellation_subtype").value("NO_SHOW"))
                .andExpect(jsonPath("$.payment_summary.section_title").value("Compensación"))
                .andExpect(jsonPath("$.payment_summary.guarantee_deposit_amount").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.payment_summary.clarifying_note")
                        .value("El sistema de pagos gestionará la compensación al propietario."))
                .andExpect(jsonPath("$.available_actions.can_cancel").value(false))
                .andExpect(jsonPath("$.contextual_banner.type").value("alert"));
    }

    @Test
    void detailCountsCompletedTripsForRenter() throws Exception { // D8: viajes COMPLETED registrados en M2
        UUID renter = UUID.randomUUID();
        completedReservation(renter);
        completedReservation(renter);

        ReservationView target = createReservation(renter, UUID.randomUUID()); // reserva consultada (INITIATED)

        mockMvc.perform(get("/api/v1/reservations/" + target.reservationId())
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renter.previous_trips").value(2))
                .andExpect(jsonPath("$.available_actions.can_initiate_payment").value(false));
    }

    @Test
    void detailOfOtherUserIs403() throws Exception { // acceso a detalle de reserva ajena retorna 403 RESERVATION_NOT_OWNED
        UUID renterA = UUID.randomUUID();
        UUID renterB = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();

        ReservationView created = createReservation(renterA, vesselId);

        mockMvc.perform(get("/api/v1/reservations/" + created.reservationId())
                        .header("X-User-Id", renterB.toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_OWNED"));
    }

    @Test
    void invalidUuidIs400() throws Exception { // identificador no uuid en ruta retorna 400 INVALID_UUID
        mockMvc.perform(get("/api/v1/reservations/not-a-valid-uuid")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID"));
    }

    @Test
    void unknownIdIs404() throws Exception { // identificador inexistente retorna 404 RESERVATION_NOT_FOUND
        mockMvc.perform(get("/api/v1/reservations/" + UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void invalidPaginationIs400() throws Exception { // parametros de paginacion invalidos retornan 400 INVALID_PARAMETERS
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .param("page", "1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "RENTER")
                        .param("page", "1")
                        .param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS"));
    }

    @Test
    void missingHeaderIs401() throws Exception { // omision de encabezados de autenticacion retorna 401 UNAUTHENTICATED
        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Role", "RENTER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(get("/api/v1/reservations/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
