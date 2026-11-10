package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.InternalReservationView;
import com.seashare.m2.reservas.application.port.out.InternalReservationReadPort;
import com.seashare.m2.reservas.domain.exception.ReservationNotFoundException;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInternalReservationServiceTest { // pruebas unitarias del servicio de lectura interno con reloj fijo

    private static final Instant FIXED_NOW = Instant.parse("2026-11-03T10:00:00Z"); // instante fijo de las pruebas

    @Mock
    private InternalReservationReadPort readPort; // puerto de lectura simulado

    @Mock
    private ClockPort clockPort; // reloj simulado

    private GetInternalReservationService service; // servicio bajo prueba

    @BeforeEach
    void setUp() { // inicializa el servicio con las dependencias simuladas
        service = new GetInternalReservationService(readPort, clockPort); // construye el servicio
    }

    private InternalReservationView view(ReservationStatus status, Instant ttlExpiresAt, LocalDate startDate,
                                         LocalDate endDate, String latestCancellationActor) { // fabrica una vista cruda
        return new InternalReservationView(
                UUID.randomUUID(), // reserva
                "RS-12345678", // codigo
                UUID.randomUUID(), // arrendatario
                UUID.randomUUID(), // embarcacion
                UUID.randomUUID(), // propietario
                startDate, // inicio
                endDate, // fin
                LocalTime.of(9, 0), // zarpe
                "America/Bogota", // zona
                2, // pasajeros
                status, // estado
                null, // subtipo
                ttlExpiresAt, // ttl
                null, // checkin
                null, // checkout
                null, // novedades
                FIXED_NOW, // creacion
                FIXED_NOW, // actualizacion
                latestCancellationActor, // actor crudo
                0, 0, null, false, null); // derivados aun sin calcular
    }

    @Test
    void missingReservationThrowsNotFound() { // reserva inexistente lanza la excepcion de dominio
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(id)) // ejecuta la consulta
                .isInstanceOf(ReservationNotFoundException.class) // valida el tipo
                .hasFieldOrPropertyWithValue("code", "RESERVATION_NOT_FOUND"); // valida el codigo
    }

    @Test
    void ttlAppliesForInitiated() { // INITIATED con ttl vigente informa segundos restantes
        UUID id = UUID.randomUUID();
        Instant expiresAt = FIXED_NOW.plusSeconds(900);
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.INITIATED, expiresAt,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), null)));
        when(clockPort.now()).thenReturn(FIXED_NOW);

        InternalReservationView result = service.execute(id);

        assertThat(result.ttlApplies()).isTrue();
        assertThat(result.ttlRemainingSeconds()).isEqualTo(900L);
        assertThat(result.ttlExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void ttlAppliesForPendingPayment() { // PENDING_PAYMENT tambien lleva temporizador
        UUID id = UUID.randomUUID();
        Instant expiresAt = FIXED_NOW.plusSeconds(120);
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.PENDING_PAYMENT, expiresAt,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), null)));
        when(clockPort.now()).thenReturn(FIXED_NOW);

        InternalReservationView result = service.execute(id);

        assertThat(result.ttlApplies()).isTrue();
        assertThat(result.ttlRemainingSeconds()).isEqualTo(120L);
    }

    @Test
    void remainingSecondsIsNeverNegative() { // ttl vencido se informa como cero
        UUID id = UUID.randomUUID();
        Instant expiresAt = FIXED_NOW.minusSeconds(30);
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.PENDING_PAYMENT, expiresAt,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), null)));
        when(clockPort.now()).thenReturn(FIXED_NOW);

        InternalReservationView result = service.execute(id);

        assertThat(result.ttlRemainingSeconds()).isZero();
    }

    @Test
    void ttlDoesNotApplyForReserved() { // RESERVED ya pago y no expone temporizador
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.RESERVED, FIXED_NOW.plusSeconds(900),
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), null)));

        InternalReservationView result = service.execute(id);

        assertThat(result.ttlApplies()).isFalse();
        assertThat(result.ttlRemainingSeconds()).isZero();
    }

    @Test
    void durationIncludesLastDay() { // del 10 al 15 de mayo son 6 dias y 5 noches
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.RESERVED, null,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), null)));

        InternalReservationView result = service.execute(id);

        assertThat(result.durationDays()).isEqualTo(6);
        assertThat(result.durationNights()).isEqualTo(5);
    }

    @Test
    void singleDayHasZeroNights() { // un solo dia cuenta como un dia y cero noches
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.RESERVED, null,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 10), null)));

        InternalReservationView result = service.execute(id);

        assertThat(result.durationDays()).isEqualTo(1);
        assertThat(result.durationNights()).isZero();
    }

    @Test
    void cancelledUsesAuditActor() { // CANCELLED expone el actor de la auditoria
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.CANCELLED, null,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), "OWNER")));

        assertThat(service.execute(id).cancellationActor()).isEqualTo("OWNER");
    }

    @Test
    void expiredUsesSystemTtlActor() { // EXPIRED informa SYSTEM_TTL
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.EXPIRED, null,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), null)));

        assertThat(service.execute(id).cancellationActor()).isEqualTo("SYSTEM_TTL");
    }

    @Test
    void otherStatusesHaveNoCancellationActor() { // otro estado no expone actor
        UUID id = UUID.randomUUID();
        when(readPort.findById(id)).thenReturn(Optional.of(view(ReservationStatus.COMPLETED, null,
                LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 15), "RENTER")));

        InternalReservationView result = service.execute(id);

        assertThat(result.cancellationActor()).isNull();
        assertThat(result.cancellationSubtype()).isNull();
    }
}
