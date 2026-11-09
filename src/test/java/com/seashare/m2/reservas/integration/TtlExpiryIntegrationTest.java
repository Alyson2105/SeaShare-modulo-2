package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.dto.ChangeReservationStatusResult;
import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.dto.StatusChangedEvent;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.in.ExpireReservationsUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.port.out.StatusAuditPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.PostgresTestContainers;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "m2.scheduler.ttl.enabled=false") // scheduler desactivado en tests para ejecucion controlada
@Import(PostgresTestContainers.class)
@Transactional
class TtlExpiryIntegrationTest { // pruebas de integracion del scheduler de TTL usando BD real y puertos/casos de uso

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso para sembrar reservas

    @Autowired
    private ExpireReservationsUseCase expireReservationsUseCase; // caso de uso bajo prueba

    @Autowired
    private ChangeReservationStatusUseCase changeStatusUseCase; // CU-08 para llevar la reserva a estados de prueba

    @Autowired
    private ReservationRepositoryPort reservationRepositoryPort; // puerto de lectura para verificar el estado final

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad operativa

    @MockitoBean
    private ClockPort clockPort; // reloj simulado para controlar el paso del tiempo

    @MockitoSpyBean
    private StatusAuditPort statusAuditPort; // espia de auditoria

    @MockitoSpyBean
    private OutboxPort outboxPort; // espia de outbox

    private static final Instant BASE_TIME = Instant.parse("2028-01-01T10:00:00Z"); // tiempo base futuro para aislar de datos previos
    private static final Duration TTL_15_MIN = Duration.ofMinutes(15); // duracion del TTL de la reserva

    @BeforeEach
    void setUp() {
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
        when(clockPort.now()).thenReturn(BASE_TIME);
        Mockito.reset(statusAuditPort, outboxPort);
    }

    private ReservationView crearReserva() { // crea una reserva en INITIATED
        return createReservationUseCase.execute(new CreateReservationCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                LocalDate.of(2028, 8, 10),
                LocalDate.of(2028, 8, 12),
                2,
                "Test TTL",
                "3000000000",
                "ttl@test.com",
                LocalTime.of(9, 0),
                new BigDecimal("500000.0000")
        ));
    }

    @Test
    void expiraReservasConTtlVencido() { // reservas cuyo TTL vencio pasan a EXPIRED
        ReservationView reserva = crearReserva();

        // Fijamos el reloj en 1 segundo despues del vencimiento de la reserva creada
        Instant despuesDeTtl = BASE_TIME.plus(TTL_15_MIN).plusSeconds(1);
        when(clockPort.now()).thenReturn(despuesDeTtl);

        int procesadas = expireReservationsUseCase.execute();

        assertThat(procesadas).isGreaterThanOrEqualTo(1);
        Reservation actualizada = reservationRepositoryPort.findById(reserva.reservationId()).orElseThrow();
        assertThat(actualizada.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void noExpiraReservasCuyoTtlNoHaVencido() { // reservas con TTL aun vigente no se expiran
        ReservationView reserva = crearReserva();

        // Fijamos el reloj en 1 segundo antes del vencimiento del TTL de la reserva creada
        Instant antesDelTtl = BASE_TIME.plus(TTL_15_MIN).minusSeconds(1);
        when(clockPort.now()).thenReturn(antesDelTtl);

        expireReservationsUseCase.execute();

        Reservation vigente = reservationRepositoryPort.findById(reserva.reservationId()).orElseThrow();
        assertThat(vigente.getStatus()).isEqualTo(ReservationStatus.INITIATED);
    }

    @Test
    void expiracionGeneraAuditoria() { // la expiracion registra la auditoria inmutable
        ReservationView reserva = crearReserva();

        Instant despuesDeTtl = BASE_TIME.plus(TTL_15_MIN).plusSeconds(1);
        when(clockPort.now()).thenReturn(despuesDeTtl);

        expireReservationsUseCase.execute();

        verify(statusAuditPort, atLeastOnce()).record(org.mockito.ArgumentMatchers.argThat(entry ->
                reserva.reservationId().equals(entry.reservationId())
                        && entry.fromStatus() == ReservationStatus.INITIATED
                        && entry.toStatus() == ReservationStatus.EXPIRED
        ));
    }

    @Test
    void noExpiraReservasSiRelojEsAnteriorACreacion() { // si el reloj es previo a la creacion, no expira la reserva
        ReservationView reserva = crearReserva();

        when(clockPort.now()).thenReturn(BASE_TIME.minusSeconds(10));

        expireReservationsUseCase.execute();

        Reservation intacta = reservationRepositoryPort.findById(reserva.reservationId()).orElseThrow();
        assertThat(intacta.getStatus()).isEqualTo(ReservationStatus.INITIATED);
    }

    @Test
    void reservedAtExactTtlCutIsRejected() { // guarda CU-08: en el segundo exacto del corte, RESERVED rechazado
        ReservationView reserva = crearReserva();
        changeStatusUseCase.execute(new ChangeReservationStatusCommand(
                reserva.reservationId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), null, null, null, null, null));
        Mockito.clearInvocations(outboxPort);

        when(clockPort.now()).thenReturn(BASE_TIME.plus(TTL_15_MIN)); // corte exacto: now == ttl_expires_at

        assertThatThrownBy(() -> changeStatusUseCase.execute(new ChangeReservationStatusCommand(
                reserva.reservationId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM, "Pago confirmado",
                null, null, null, null, null, "pg-999")))
                .isInstanceOf(ReservationRuleException.class)
                .hasFieldOrPropertyWithValue("code", "RESERVATION_EXPIRED");

        Reservation sinCambiar = reservationRepositoryPort.findById(reserva.reservationId()).orElseThrow();
        assertThat(sinCambiar.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(outboxPort, never()).enqueue(any(StatusChangedEvent.class));
    }

    @Test
    void reservedOneSecondBeforeTtlCutIsAccepted() { // 1 segundo antes del corte, RESERVED valido
        ReservationView reserva = crearReserva();
        changeStatusUseCase.execute(new ChangeReservationStatusCommand(
                reserva.reservationId(), ReservationStatus.PENDING_PAYMENT, null, ActorType.RENTER, "Pago iniciado",
                UUID.randomUUID(), null, null, null, null, null));

        when(clockPort.now()).thenReturn(BASE_TIME.plus(TTL_15_MIN).minusSeconds(1)); // 1 s antes del corte

        ChangeReservationStatusResult result = changeStatusUseCase.execute(new ChangeReservationStatusCommand(
                reserva.reservationId(), ReservationStatus.RESERVED, null, ActorType.SYSTEM, "Pago confirmado",
                null, null, null, null, null, "pg-1000"));

        assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(result.eventsEnqueued()).containsExactly("AMQP");
    }
}
