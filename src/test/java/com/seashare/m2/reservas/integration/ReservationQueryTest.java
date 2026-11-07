package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
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
class ReservationQueryTest { // pruebas de integracion de endpoints de consulta usando MockMvc y el caso de uso de creacion

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para realizar peticiones rest

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso para sembrar reservas

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota requerida por el contexto

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad operativa

    private UUID ownerId; // propietario devuelto por el mock de flota

    @BeforeEach
    void setUp() { // configuracion inicial de mocks de flota
        ownerId = UUID.randomUUID();
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
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
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total_items").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].reservation_id").isNotEmpty())
                .andExpect(jsonPath("$.items[1].reservation_id").isNotEmpty());

        mockMvc.perform(get("/api/v1/reservations")
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "OWNER")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_items").value(3))
                .andExpect(jsonPath("$.items.length()").value(3));
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
