package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationInfoProvidedEvent;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.OutboxPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.NewReservation;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReservationServiceTest { // pruebas unitarias aisladas de create reservation service y encolado outbox

    @Mock
    private ClockPort clockPort; // simulacion del puerto de reloj

    @Mock
    private IdempotencyPort idempotencyPort; // simulacion del puerto de idempotencia

    @Mock
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de informacion de flota

    @Mock
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion del puerto de estado operativo

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort; // simulacion del puerto del repositorio

    @Mock
    private OutboxPort outboxPort; // simulacion del puerto outbox

    private CreateReservationService service; // servicio bajo prueba

    private final Instant fixedNow = Instant.parse("2026-11-06T12:00:00Z"); // instante temporal fijo de prueba
    private final UUID ownerId = UUID.randomUUID(); // id de propietario de prueba
    private final UUID vesselId = UUID.randomUUID(); // id de embarcacion de prueba
    private final UUID renterId = UUID.randomUUID(); // id de arrendatario de prueba

    @BeforeEach
    void setUp() { // inicializa servicio con los mocks inyectados
        service = new CreateReservationService(
                clockPort,
                idempotencyPort,
                fleetInfoPort,
                fleetOperationalStatusPort,
                reservationRepositoryPort,
                outboxPort
        );
        when(clockPort.now()).thenReturn(fixedNow); // reloj devuelve instante fijo
    }

    private CreateReservationCommand buildCommand(String idempotencyKey, int passengers) { // crea comando de prueba
        return new CreateReservationCommand(
                renterId,
                idempotencyKey,
                vesselId,
                LocalDate.of(2027, 4, 1),
                LocalDate.of(2027, 4, 3),
                passengers,
                "Pedro Gomez",
                "+573001234567",
                "pedro@example.com",
                LocalTime.of(8, 30),
                new BigDecimal("2500000.00")
        );
    }

    @Test
    void createsReservationAndEnqueuesOutboxEventWithDeterministicEventId() { // creacion exitosa encola evento con event_id deterministico
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 4);

        when(idempotencyPort.tryClaim(eq(key), eq(CreateReservationService.IDEMPOTENCY_ENDPOINT), eq(fixedNow), any()))
                .thenReturn(true);
        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(true);
        when(reservationRepositoryPort.existsOverlap(eq(vesselId), any(), any(), any())).thenReturn(false);

        NewReservation newReservation = new NewReservation(
                renterId, vesselId, ownerId, command.startDate(), command.endDate(),
                4, command.holderName(), command.holderPhone(), command.holderEmail(),
                command.estimatedTotal(), command.departureTime(), "America/Bogota",
                fixedNow, fixedNow.plusSeconds(900)
        );
        Reservation savedReservation = Reservation.initiate(newReservation);
        when(reservationRepositoryPort.create(any())).thenReturn(savedReservation);

        ReservationView result = service.execute(command); // ejecuta creacion

        assertThat(result).isNotNull(); // valida vista devuelta

        ArgumentCaptor<ReservationInfoProvidedEvent> captor = ArgumentCaptor.forClass(ReservationInfoProvidedEvent.class); // captura evento
        verify(outboxPort).enqueue(captor.capture()); // verifica encolado en outbox
        ReservationInfoProvidedEvent event = captor.getValue(); // obtiene evento encolado

        UUID expectedEventId = UUID.nameUUIDFromBytes(("reservation.info.provided:" + savedReservation.getId()).getBytes(StandardCharsets.UTF_8)); // calcula id deterministico
        assertThat(event.eventId()).isEqualTo(expectedEventId); // verifica que event_id sea deterministico sin randomUUID
        assertThat(event.reservationId()).isEqualTo(savedReservation.getId()); // verifica id de reserva
        assertThat(event.vesselId()).isEqualTo(vesselId); // verifica id de embarcacion
        assertThat(event.startDate()).isEqualTo(LocalDate.of(2027, 4, 1)); // verifica fecha de inicio
        assertThat(event.endDate()).isEqualTo(LocalDate.of(2027, 4, 3)); // verifica fecha de fin
        assertThat(event.passengers()).isEqualTo(4); // verifica pasajeros
        assertThat(event.ownerId()).isEqualTo(ownerId); // verifica propietario
        assertThat(event.maxCapacity()).isEqualTo(10); // verifica capacidad maxima
        assertThat(event.occurredAt()).isEqualTo(fixedNow); // verifica timestamp proveniente de clockPort

        verify(idempotencyPort).complete(eq(key), eq(CreateReservationService.IDEMPOTENCY_ENDPOINT), eq(savedReservation.getId())); // verifica asociacion de clave
    }

    @Test
    void idempotentRepeatDoesNotEnqueueOutboxEvent() { // repeticion idempotente devuelve reserva existente y no encola evento
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 4);
        UUID existingReservationId = UUID.randomUUID();

        when(idempotencyPort.tryClaim(eq(key), eq(CreateReservationService.IDEMPOTENCY_ENDPOINT), eq(fixedNow), any()))
                .thenReturn(false);
        when(idempotencyPort.findReservationId(eq(key), eq(CreateReservationService.IDEMPOTENCY_ENDPOINT)))
                .thenReturn(Optional.of(existingReservationId));

        NewReservation existingData = new NewReservation(
                renterId, vesselId, ownerId, command.startDate(), command.endDate(),
                4, command.holderName(), command.holderPhone(), command.holderEmail(),
                command.estimatedTotal(), command.departureTime(), "America/Bogota",
                fixedNow, fixedNow.plusSeconds(900)
        );
        Reservation existingReservation = Reservation.initiate(existingData);
        when(reservationRepositoryPort.findById(existingReservationId)).thenReturn(Optional.of(existingReservation));

        ReservationView result = service.execute(command); // ejecuta peticion repetida

        assertThat(result).isNotNull(); // valida respuesta
        verify(outboxPort, never()).enqueue(any(ReservationInfoProvidedEvent.class)); // garantiza que no se encolo otro mensaje
        verify(reservationRepositoryPort, never()).create(any()); // garantiza que no se creo nueva reserva
    }

    @Test
    void whenCreationFailsOutboxIsNotEnqueued() { // si falla validacion de capacidad no se encola evento outbox
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 12); // pasajeros superan capacidad 8

        when(idempotencyPort.tryClaim(eq(key), eq(CreateReservationService.IDEMPOTENCY_ENDPOINT), eq(fixedNow), any()))
                .thenReturn(true);
        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(ownerId, 8, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(command)) // valida lanzamiento de excepcion de capacidad
                .isInstanceOf(ReservationRuleException.class);

        verify(outboxPort, never()).enqueue(any(ReservationInfoProvidedEvent.class)); // garantiza que no se encola mensaje ante fallo
    }
}
