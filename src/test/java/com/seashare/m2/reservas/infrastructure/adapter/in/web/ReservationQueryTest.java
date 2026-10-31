package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.application.port.out.FleetInfo;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.infrastructure.adapter.out.persistence.ReservationEntity;
import com.seashare.m2.reservas.infrastructure.adapter.out.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationQueryTest { // pruebas de integracion para endpoints de consulta paginada y detalle de reservas

    @Autowired
    private MockMvc mockMvc; // cliente mockmvc para realizar peticiones rest

    @Autowired
    private ReservationJpaRepository reservationJpaRepository; // repositorio jpa para insercion directa de entidades

    @Autowired
    private JdbcTemplate jdbcTemplate; // cliente jdbc para eliminacion por vessel_id sin alterar triggers

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota requerida por el contexto

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad operativa

    private final List<UUID> trackedVessels = Collections.synchronizedList(new ArrayList<>()); // lista de embarcaciones para limpieza

    @BeforeEach
    void setUp() { // configuracion inicial de mocks de flota
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota")); // mock de flota
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true); // mock de operatividad
    }

    @AfterEach
    void tearDown() { // eliminacion limpia de registros creados durante la prueba
        for (UUID vesselId : trackedVessels) { // itera embarcaciones registradas
            jdbcTemplate.update("DELETE FROM reservation WHERE vessel_id = ?", vesselId); // elimina reservas de la embarcacion
        }
        trackedVessels.clear(); // limpia coleccion de embarcaciones
    }

    private ReservationEntity createReservation(UUID renterId, UUID ownerId, UUID vesselId) { // helper para insercion directa en base de datos
        ReservationEntity entity = new ReservationEntity(); // nueva entidad jpa
        entity.setReservationCode("RS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()); // codigo unico
        entity.setRenterId(renterId); // asigna arrendatario
        entity.setOwnerId(ownerId); // asigna propietario
        entity.setVesselId(vesselId); // asigna embarcacion
        entity.setStatus(ReservationStatus.INITIATED); // estado inicial iniciado
        entity.setStartDate(LocalDate.of(2027, 4, 10)); // fecha inicial de prueba
        entity.setEndDate(LocalDate.of(2027, 4, 12)); // fecha final de prueba
        entity.setDepartureTime(LocalTime.of(8, 30)); // hora zarpe pactada
        entity.setPassengers(2); // cantidad de pasajeros
        entity.setHolderName("Juan Perez"); // titular
        entity.setHolderPhone("+573001234567"); // telefono
        entity.setHolderEmail("juan@example.com"); // correo
        entity.setEstimatedTotal(new BigDecimal("5000000.0000")); // total estimado
        entity.setCurrency("COP"); // moneda
        entity.setM1Blocked(false); // flag m1
        entity.setCreatedAt(Instant.now()); // marca temporal de creacion
        entity.setHarborTimezone("America/Bogota"); // zona horaria valida
        entity.setTtlExpiresAt(Instant.now().plus(Duration.ofMinutes(15))); // expiracion ttl
        return reservationJpaRepository.saveAndFlush(entity); // persiste directamente y asegura commit inmediato
    }

    @Test
    void listsOnlyOwnReservations() throws Exception { // consulta paginada retorna exclusivamente reservas propias del llamador
        UUID userRenter = UUID.randomUUID(); // identificador del arrendatario
        UUID otherRenter = UUID.randomUUID(); // identificador de otro arrendatario
        UUID ownerId = UUID.randomUUID(); // identificador del propietario
        UUID vesselId = UUID.randomUUID(); // identificador de embarcacion
        trackedVessels.add(vesselId); // registra embarcacion para limpieza

        createReservation(userRenter, ownerId, vesselId); // reserva propia uno
        createReservation(userRenter, ownerId, vesselId); // reserva propia dos
        createReservation(otherRenter, ownerId, vesselId); // reserva ajena de otro arrendatario

        mockMvc.perform(get("/api/v1/reservations") // ejecuta peticion get para arrendatario
                        .header("X-User-Id", userRenter.toString()) // encabezado de identidad
                        .header("X-User-Role", "RENTER") // rol arrendatario
                        .param("page", "1") // pagina uno
                        .param("limit", "10")) // limite diez
                .andExpect(status().isOk()) // valida status 200 ok
                .andExpect(jsonPath("$.page").value(1)) // valida numero de pagina
                .andExpect(jsonPath("$.limit").value(10)) // valida limite de elementos
                .andExpect(jsonPath("$.total_items").value(2)) // valida cantidad total de elementos propios
                .andExpect(jsonPath("$.items.length()").value(2)) // valida tamano de la coleccion devuelta
                .andExpect(jsonPath("$.items[0].reservation_id").isNotEmpty()) // valida presencia de id en primer elemento
                .andExpect(jsonPath("$.items[1].reservation_id").isNotEmpty()); // valida presencia de id en segundo elemento

        mockMvc.perform(get("/api/v1/reservations") // ejecuta peticion get para propietario
                        .header("X-User-Id", ownerId.toString()) // encabezado del propietario
                        .header("X-User-Role", "OWNER") // rol propietario
                        .param("page", "1") // pagina uno
                        .param("limit", "10")) // limite diez
                .andExpect(status().isOk()) // valida status 200 ok
                .andExpect(jsonPath("$.total_items").value(3)) // valida que el propietario ve las tres reservas de su barco
                .andExpect(jsonPath("$.items.length()").value(3)); // valida cantidad de elementos devueltos
    }

    @Test
    void detailOfOtherUserIs403() throws Exception { // acceso a detalle de reserva ajena retorna 403 RESERVATION_NOT_OWNED
        UUID ownerId = UUID.randomUUID(); // propietario
        UUID renterA = UUID.randomUUID(); // arrendatario dueno
        UUID renterB = UUID.randomUUID(); // arrendatario ajeno
        UUID vesselId = UUID.randomUUID(); // embarcacion
        trackedVessels.add(vesselId); // registra para limpieza

        ReservationEntity entity = createReservation(renterA, ownerId, vesselId); // reserva creada para arrendatario A

        mockMvc.perform(get("/api/v1/reservations/" + entity.getId()) // intenta consultar detalle con usuario B
                        .header("X-User-Id", renterB.toString()) // identidad del usuario B
                        .header("X-User-Role", "RENTER")) // rol arrendatario
                .andExpect(status().isForbidden()) // valida status 403 forbidden
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_OWNED")); // valida codigo RESERVATION_NOT_OWNED
    }

    @Test
    void invalidUuidIs400() throws Exception { // identificador no uuid en ruta retorna 400 INVALID_UUID
        mockMvc.perform(get("/api/v1/reservations/not-a-valid-uuid") // peticion con formato no uuid
                        .header("X-User-Id", UUID.randomUUID().toString()) // usuario autenticado
                        .header("X-User-Role", "RENTER")) // rol arrendatario
                .andExpect(status().isBadRequest()) // valida status 400 bad request
                .andExpect(jsonPath("$.code").value("INVALID_UUID")); // valida codigo INVALID_UUID
    }

    @Test
    void unknownIdIs404() throws Exception { // identificador inexistente retorna 404 RESERVATION_NOT_FOUND
        UUID nonExistentId = UUID.randomUUID(); // uuid aleatorio inexistente

        mockMvc.perform(get("/api/v1/reservations/" + nonExistentId) // peticion con reserva no registrada
                        .header("X-User-Id", UUID.randomUUID().toString()) // usuario autenticado
                        .header("X-User-Role", "RENTER")) // rol arrendatario
                .andExpect(status().isNotFound()) // valida status 404 not found
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND")); // valida codigo RESERVATION_NOT_FOUND
    }

    @Test
    void invalidPaginationIs400() throws Exception { // parametros de paginacion invalidos retornan 400 INVALID_PARAMETERS
        UUID userId = UUID.randomUUID(); // usuario valido

        mockMvc.perform(get("/api/v1/reservations") // peticion con page menor a uno
                        .header("X-User-Id", userId.toString()) // identidad
                        .header("X-User-Role", "RENTER") // rol
                        .param("page", "0") // page cero invalido
                        .param("limit", "10")) // limit valido
                .andExpect(status().isBadRequest()) // valida 400 bad request
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida INVALID_PARAMETERS

        mockMvc.perform(get("/api/v1/reservations") // peticion con limit menor a uno
                        .header("X-User-Id", userId.toString()) // identidad
                        .header("X-User-Role", "RENTER") // rol
                        .param("page", "1") // page valido
                        .param("limit", "0")) // limit cero invalido
                .andExpect(status().isBadRequest()) // valida 400 bad request
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida INVALID_PARAMETERS

        mockMvc.perform(get("/api/v1/reservations") // peticion con limit mayor a cincuenta
                        .header("X-User-Id", userId.toString()) // identidad
                        .header("X-User-Role", "RENTER") // rol
                        .param("page", "1") // page valido
                        .param("limit", "51")) // limit cincuenta y uno invalido
                .andExpect(status().isBadRequest()) // valida 400 bad request
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETERS")); // valida INVALID_PARAMETERS
    }

    @Test
    void missingHeaderIs401() throws Exception { // omision de encabezados de autenticacion retorna 401 UNAUTHENTICATED
        mockMvc.perform(get("/api/v1/reservations") // peticion sin encabezados
                        .header("X-User-Role", "RENTER")) // solo rol sin X-User-Id
                .andExpect(status().isUnauthorized()) // valida 401 unauthorized
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED")); // valida UNAUTHENTICATED

        mockMvc.perform(get("/api/v1/reservations") // peticion sin X-User-Role
                        .header("X-User-Id", UUID.randomUUID().toString())) // solo id sin rol
                .andExpect(status().isUnauthorized()) // valida 401 unauthorized
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED")); // valida UNAUTHENTICATED

        mockMvc.perform(get("/api/v1/reservations/" + UUID.randomUUID())) // detalle sin ningun encabezado
                .andExpect(status().isUnauthorized()) // valida 401 unauthorized
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED")); // valida UNAUTHENTICATED
    }
}
