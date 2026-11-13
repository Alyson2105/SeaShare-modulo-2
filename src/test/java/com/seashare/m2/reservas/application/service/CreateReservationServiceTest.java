package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.InitiateReservationCommand;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.ReservationRuleException;
import com.seashare.m2.reservas.domain.model.Reservation;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReservationServiceTest { // T-24: la creacion delega la escritura en CU-08, nunca persiste por su cuenta

    @Mock
    private ClockPort clockPort; // simulacion del puerto de reloj

    @Mock
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de informacion de flota

    @Mock
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion del puerto de estado operativo

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort; // simulacion del puerto del repositorio

    @Mock
    private CreateReservationWriter writer; // simulacion de la frontera transaccional

    private CreateReservationService service; // servicio bajo prueba

    private final Instant fixedNow = Instant.parse("2026-11-06T12:00:00Z"); // instante temporal fijo de prueba
    private final UUID ownerId = UUID.randomUUID(); // id de propietario de prueba
    private final UUID vesselId = UUID.randomUUID(); // id de embarcacion de prueba
    private final UUID renterId = UUID.randomUUID(); // id de arrendatario de prueba

    @BeforeEach
    void setUp() { // inicializa servicio con los mocks inyectados
        service = new CreateReservationService(
                clockPort,
                fleetInfoPort,
                fleetOperationalStatusPort,
                reservationRepositoryPort,
                writer);
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

    private Reservation persisted(UUID id) { // reserva ya persistida con id asignado
        return Reservation.builder()
                .id(id)
                .renterId(renterId)
                .ownerId(ownerId)
                .vesselId(vesselId)
                .startDate(LocalDate.of(2027, 4, 1))
                .endDate(LocalDate.of(2027, 4, 3))
                .passengers(4)
                .holderName("Pedro Gomez")
                .holderPhone("+573001234567")
                .holderEmail("pedro@example.com")
                .estimatedTotal(new BigDecimal("2500000.00"))
                .departureTime(LocalTime.of(8, 30))
                .harborTimezone("America/Bogota")
                .ttlExpiresAt(fixedNow.plusSeconds(900))
                .createdAt(fixedNow)
                .status(ReservationStatus.INITIATED)
                .version(0L)
                .build();
    }

    @Test
    void creationDelegatesToInitiateUseCase() { // la creacion no persiste directamente: delega en la transaccion de CU-08
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 4);
        Reservation created = persisted(UUID.randomUUID());

        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(true);
        when(reservationRepositoryPort.existsOverlap(eq(vesselId), any(), any(), any())).thenReturn(false);
        when(writer.createOrReturnExisting(eq(key), eq(fixedNow), eq(CreateReservationService.IDEMPOTENCY_WINDOW), any()))
                .thenReturn(created);

        ReservationView result = service.execute(command); // ejecuta creacion

        assertThat(result.reservationId()).isEqualTo(created.getId()); // devuelve la reserva creada
        verify(writer).createOrReturnExisting(eq(key), eq(fixedNow), eq(CreateReservationService.IDEMPOTENCY_WINDOW), any());
        verify(reservationRepositoryPort, never()).create(any()); // ya no hay escritura directa fuera de CU-08
    }

    @Test
    void commandCarriesMaxCapacityFromModuleOne() { // max_capacity viaja a CU-08 para reservation.info.provided
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 4);

        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(true);
        when(reservationRepositoryPort.existsOverlap(eq(vesselId), any(), any(), any())).thenReturn(false);
        when(writer.createOrReturnExisting(any(), any(), any(), any())).thenReturn(persisted(UUID.randomUUID()));

        service.execute(command); // ejecuta creacion

        ArgumentCaptor<InitiateReservationCommand> captor = ArgumentCaptor.forClass(InitiateReservationCommand.class);
        verify(writer).createOrReturnExisting(any(), any(), any(), captor.capture());

        assertThat(captor.getValue().maxCapacity()).isEqualTo(10); // capacidad de M1
        assertThat(captor.getValue().newReservation().renterId()).isEqualTo(renterId); // titular
        assertThat(captor.getValue().newReservation().estimatedTotal()).isEqualByComparingTo("2500000.00"); // monto literal
        assertThat(captor.getValue().newReservation().ttlExpiresAt()).isEqualTo(fixedNow.plusSeconds(900)); // ttl de 15 min
        assertThat(captor.getValue().newReservation().createdAt()).isEqualTo(fixedNow); // misma marca temporal
    }

    @Test
    void idempotentRepeatReturnsExistingReservation() { // la repeticion la resuelve la transaccion sin crear otra reserva
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 4);
        Reservation existing = persisted(UUID.randomUUID());

        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(ownerId, 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(true);
        when(reservationRepositoryPort.existsOverlap(eq(vesselId), any(), any(), any())).thenReturn(false);
        when(writer.createOrReturnExisting(any(), any(), any(), any())).thenReturn(existing);

        ReservationView result = service.execute(command); // ejecuta peticion repetida

        assertThat(result.reservationId()).isEqualTo(existing.getId()); // devuelve la reserva original
        verify(reservationRepositoryPort, never()).create(any()); // no se creo una segunda reserva
    }

    @Test
    void whenCapacityIsExceededWriterIsNotCalled() { // fallo de validacion antes de abrir la transaccion
        String key = UUID.randomUUID().toString();
        CreateReservationCommand command = buildCommand(key, 12); // pasajeros superan la capacidad 8

        when(fleetInfoPort.getVesselInfo(vesselId)).thenReturn(new FleetInfo(ownerId, 8, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(vesselId)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(command)) // valida lanzamiento de excepcion de capacidad
                .isInstanceOf(ReservationRuleException.class);

        verify(writer, never()).createOrReturnExisting(any(), any(), any(), any()); // no se escribe nada
    }
}
