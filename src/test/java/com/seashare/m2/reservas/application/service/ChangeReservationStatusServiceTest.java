package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.reservas.domain.state.InvalidTransitionException;
import com.seashare.m2.reservas.domain.state.ReservationStateMachine;
import com.seashare.m2.reservas.infrastructure.adapter.out.persistence.ReservationEntity;
import com.seashare.m2.reservas.infrastructure.adapter.out.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ChangeReservationStatusServiceTest { // pruebas de integracion para el servicio de cambio de estado

    @Autowired
    private ChangeReservationStatusService service; // servicio bajo prueba

    @Autowired
    private ReservationJpaRepository repository; // repositorio jpa para preparar datos

    @Autowired
    private ReservationRepositoryPort reservationPort; // puerto de repositorio para pruebas de concurrencia

    @Autowired
    private JdbcTemplate jdbcTemplate; // cliente jdbc para conteo de tablas secundarias

    @MockitoSpyBean
    private OutboxPort outboxPort; // espia sobre el puerto outbox

    @AfterEach
    void tearDown() { // resetea el espia despues de cada prueba
        Mockito.reset(outboxPort); // restablece comportamiento original del espia
    }

    private ReservationEntity createValidEntity() { // fabrica entidad valida para pruebas
        ReservationEntity entity = new ReservationEntity(); // instancia de entidad
        entity.setReservationCode("RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)); // codigo unico
        entity.setRenterId(UUID.randomUUID()); // arrendatario aleatorio
        entity.setVesselId(UUID.randomUUID()); // embarcacion aleatoria
        entity.setOwnerId(UUID.randomUUID()); // propietario aleatorio
        entity.setStatus(ReservationStatus.INITIATED); // estado inicial
        entity.setStartDate(LocalDate.now().plusDays(5)); // fecha inicio
        entity.setEndDate(LocalDate.now().plusDays(7)); // fecha fin
        entity.setDepartureTime(LocalTime.of(9, 0)); // hora de zarpe
        entity.setPassengers(2); // cantidad pasajeros
        entity.setHolderName("Test"); // nombre titular
        entity.setCurrency("COP"); // moneda
        entity.setM1Blocked(false); // indicador m1
        entity.setHarborTimezone("America/Bogota"); // zona horaria del puerto
        entity.setEstimatedTotal(new BigDecimal("1000000.0000")); // total estimado
        entity.setRentalAmount(new BigDecimal("1000000.0000")); // monto alquiler
        entity.setInsuranceAmount(new BigDecimal("1000000.0000")); // monto seguro
        entity.setGuaranteeDepositAmount(new BigDecimal("1000000.0000")); // monto garantia
        entity.setCreatedAt(Instant.now()); // marca temporal de creacion
        return entity; // retorna entidad preparada
    }

    private int countAudit(UUID reservationId) { // cuenta registros de auditoria para la reserva
        Integer count = jdbcTemplate.queryForObject( // ejecuta consulta de conteo
                "SELECT count(*) FROM reservation_status_audit WHERE reservation_id = ?", // sql de conteo
                Integer.class, // tipo retornado
                reservationId // parametro de reserva
        );
        return count != null ? count : 0; // retorna conteo seguro
    }

    private int countOutbox(UUID reservationId) { // cuenta mensajes outbox asociados a la reserva
        Integer count = jdbcTemplate.queryForObject( // ejecuta consulta de conteo
                "SELECT count(*) FROM outbox_message WHERE payload LIKE ?", // sql de conteo por patron
                Integer.class, // tipo retornado
                "%" + reservationId + "%" // patron con id de reserva
        );
        return count != null ? count : 0; // retorna conteo seguro
    }

    @Test
    void validChangeSavesStatusAndAudit() { // transicion valida persiste estado auditoria y outbox
        ReservationEntity saved = repository.save(createValidEntity()); // inserta reserva en estado INITIATED
        Long originalVersion = saved.getVersion(); // guarda version inicial

        ChangeReservationStatusCommand command = new ChangeReservationStatusCommand( // construye comando valido
                saved.getId(), // id de la reserva
                ReservationStatus.PENDING_PAYMENT, // estado destino
                null, // sin subtipo de cancelacion
                ActorType.RENTER, // actor arrendatario
                "Iniciando flujo de pago" // razon descriptiva
        );

        service.execute(command); // ejecuta servicio transaccional

        ReservationEntity updated = repository.findById(saved.getId()).orElseThrow(); // recupera entidad actualizada
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT); // valida nuevo estado
        assertThat(updated.getVersion()).isGreaterThan(originalVersion); // valida incremento de version
        assertThat(countAudit(saved.getId())).isEqualTo(1); // verifica insercion de auditoria
        assertThat(countOutbox(saved.getId())).isEqualTo(1); // verifica insercion en outbox
    }

    @Test
    void invalidChangeWritesNothing() { // transicion invalida no modifica estado ni inserta filas
        ReservationEntity saved = repository.save(createValidEntity()); // inserta reserva inicial
        Long originalVersion = saved.getVersion(); // version previa

        ChangeReservationStatusCommand command = new ChangeReservationStatusCommand( // comando con salto invalido
                saved.getId(), // id de la reserva
                ReservationStatus.COMPLETED, // estado destino no alcanzable directamente
                null, // sin subtipo
                ActorType.RENTER, // actor
                "Intento de salto directo a completed" // razon
        );

        assertThatThrownBy(() -> service.execute(command)) // ejecuta operacion que debe fallar
                .isInstanceOf(InvalidTransitionException.class); // espera excepcion de transicion invalida

        ReservationEntity unchanged = repository.findById(saved.getId()).orElseThrow(); // recupera entidad
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.INITIATED); // valida estado intacto
        assertThat(unchanged.getVersion()).isEqualTo(originalVersion); // valida version sin cambio
        assertThat(countAudit(saved.getId())).isEqualTo(0); // verifica sin filas de auditoria
        assertThat(countOutbox(saved.getId())).isEqualTo(0); // verifica sin filas de outbox

        repository.deleteById(saved.getId()); // limpia registro sin auditoria asociada
    }

    @Test
    void versionConflictRaisesVERSION_CONFLICT() { // version desactualizada lanza conflicto de version
        ReservationEntity saved = repository.save(createValidEntity()); // inserta reserva inicial
        Reservation b = reservationPort.findById(saved.getId()).orElseThrow(); // carga copia vieja b

        ChangeReservationStatusCommand command = new ChangeReservationStatusCommand( // comando concurrente
                saved.getId(), // id de la reserva
                ReservationStatus.PENDING_PAYMENT, // estado destino
                null, // sin subtipo
                ActorType.RENTER, // actor
                "Cambio por flujo principal" // razon
        );
        service.execute(command); // ejecuta cambio incrementando version en base de datos

        b.changeStatus(ReservationStatus.EXPIRED, null, new ReservationStateMachine()); // aplica transicion sobre copia b

        assertThatThrownBy(() -> reservationPort.save(b)) // intenta guardar copia con version vieja
                .isInstanceOf(VersionConflictException.class) // espera excepcion de conflicto
                .matches(ex -> "VERSION_CONFLICT".equals(((VersionConflictException) ex).getCode())); // valida codigo esperado
    }

    @Test
    void failureRollsEverythingBack() { // fallo en outbox revierte cambios en estado y auditoria
        doThrow(new RuntimeException("boom")).when(outboxPort).enqueue(any()); // fuerza error en encolado

        ReservationEntity saved = repository.save(createValidEntity()); // inserta reserva inicial
        Long originalVersion = saved.getVersion(); // version previa

        ChangeReservationStatusCommand command = new ChangeReservationStatusCommand( // comando valido
                saved.getId(), // id de la reserva
                ReservationStatus.PENDING_PAYMENT, // estado destino
                null, // sin subtipo
                ActorType.RENTER, // actor
                "Fallo provocado en outbox" // razon
        );

        assertThatThrownBy(() -> service.execute(command)) // ejecuta esperando error forzado
                .isInstanceOf(RuntimeException.class) // espera runtime exception
                .hasMessage("boom"); // valida mensaje del error

        ReservationEntity unchanged = repository.findById(saved.getId()).orElseThrow(); // recupera entidad tras rollback
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.INITIATED); // valida estado no modificado
        assertThat(unchanged.getVersion()).isEqualTo(originalVersion); // valida version no alterada
        assertThat(countAudit(saved.getId())).isEqualTo(0); // valida rollback de auditoria
        assertThat(countOutbox(saved.getId())).isEqualTo(0); // valida que no hubo mensaje outbox

        repository.deleteById(saved.getId()); // limpia registro sin auditoria asociada
    }

    @Test
    void actorNotAllowedForSubtypeIsRejected() { // actor no permitido para subtipo es rechazado
        ReservationEntity entity = createValidEntity(); // prepara entidad valida
        entity.setStatus(ReservationStatus.RESERVED); // fija estado inicial en RESERVED
        ReservationEntity saved = repository.save(entity); // persiste reserva reservada

        ChangeReservationStatusCommand command = new ChangeReservationStatusCommand( // comando no autorizado
                saved.getId(), // id de la reserva
                ReservationStatus.CANCELLED, // destino cancelado
                CancellationSubtype.BY_OWNER, // subtipo exclusivo de propietario
                ActorType.RENTER, // actor arrendatario no permitido para este subtipo
                "Cancelacion no permitida por arrendatario" // razon
        );

        assertThatThrownBy(() -> service.execute(command)) // ejecuta esperando rechazo
                .isInstanceOf(InvalidTransitionException.class); // espera transicion invalida

        ReservationEntity unchanged = repository.findById(saved.getId()).orElseThrow(); // recupera entidad
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.RESERVED); // valida estado RESERVED intacto
        assertThat(countAudit(saved.getId())).isEqualTo(0); // valida cero auditoria
        assertThat(countOutbox(saved.getId())).isEqualTo(0); // valida cero outbox

        repository.deleteById(saved.getId()); // limpia registro sin auditoria asociada
    }
}
