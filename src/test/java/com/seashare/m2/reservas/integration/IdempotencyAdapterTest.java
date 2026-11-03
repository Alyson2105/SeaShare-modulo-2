package com.seashare.m2.reservas.integration;

import com.seashare.m2.reservas.application.dto.CreateReservationCommand;
import com.seashare.m2.reservas.application.dto.FleetInfo;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.application.port.in.CreateReservationUseCase;
import com.seashare.m2.reservas.application.port.out.FleetInfoPort;
import com.seashare.m2.reservas.application.port.out.FleetOperationalStatusPort;
import com.seashare.m2.reservas.application.port.out.IdempotencyPort;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.application.service.CreateReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class IdempotencyAdapterTest { // pruebas de comportamiento de idempotencia a traves del puerto y del caso de uso de creacion

    private static final String ENDPOINT = CreateReservationService.IDEMPOTENCY_ENDPOINT; // endpoint logico usado por el caso de uso real
    private static final Duration WINDOW = CreateReservationService.IDEMPOTENCY_WINDOW; // ventana de expiracion usada por el caso de uso real

    @Autowired
    private CreateReservationUseCase createReservationUseCase; // caso de uso bajo prueba

    @Autowired
    private ReservationRepositoryPort reservationRepositoryPort; // puerto de lectura para verificar reservas creadas

    @Autowired
    private IdempotencyPort idempotencyPort; // puerto bajo prueba para reclamar y completar claves

    @MockitoBean
    private FleetInfoPort fleetInfoPort; // simulacion del puerto de flota de modulo 1

    @MockitoBean
    private FleetOperationalStatusPort fleetOperationalStatusPort; // simulacion de disponibilidad operativa

    @BeforeEach
    void setUp() { // prepara mocks de modulo 1 antes de cada prueba
        when(fleetInfoPort.getVesselInfo(any())).thenReturn(new FleetInfo(UUID.randomUUID(), 10, "America/Bogota"));
        when(fleetOperationalStatusPort.isAvailable(any())).thenReturn(true);
    }

    private CreateReservationCommand command(UUID renterId, String idempotencyKey, UUID vesselId) { // construye un comando valido
        return new CreateReservationCommand(
                renterId,
                idempotencyKey,
                vesselId,
                LocalDate.of(2027, 6, 10),
                LocalDate.of(2027, 6, 12),
                2,
                "Ana",
                "3000000000",
                "ana@test.com",
                LocalTime.of(9, 0),
                new BigDecimal("100.0000")
        );
    }

    @Test
    void sameIdempotencyKeyReturnsSameReservationAndCreatesOnlyOne() { // misma clave dos veces devuelve la misma reserva y crea una sola
        UUID renterId = UUID.randomUUID();
        CreateReservationCommand command = command(renterId, UUID.randomUUID().toString(), UUID.randomUUID());

        ReservationView first = createReservationUseCase.execute(command);
        ReservationView second = createReservationUseCase.execute(command);

        assertThat(second.reservationId()).isEqualTo(first.reservationId());
        assertThat(reservationRepositoryPort.findByRenter(renterId, 1, 10).totalItems()).isEqualTo(1);
    }

    @Test
    void differentIdempotencyKeysCreateDifferentReservations() { // claves distintas crean reservas distintas
        UUID renterId = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();

        ReservationView first = createReservationUseCase.execute(command(renterId, UUID.randomUUID().toString(), vesselId));
        ReservationView second = createReservationUseCase.execute(command(renterId, UUID.randomUUID().toString(), vesselId));

        assertThat(second.reservationId()).isNotEqualTo(first.reservationId());
        assertThat(reservationRepositoryPort.findByRenter(renterId, 1, 10).totalItems()).isEqualTo(2);
    }

    @Test
    void newKeyIsClaimedAndSameKeyWithinWindowIsRejected() { // clave nueva se reclama; la misma clave dentro de la ventana no
        String key = UUID.randomUUID().toString();
        Instant now = Instant.now();

        assertThat(idempotencyPort.tryClaim(key, ENDPOINT, now, WINDOW)).isTrue();
        assertThat(idempotencyPort.tryClaim(key, ENDPOINT, now.plusSeconds(1), WINDOW)).isFalse();
    }

    @Test
    void expiredKeyCanBeClaimedAgain() { // una clave vencida vuelve a ser reclamable con un now posterior
        String key = UUID.randomUUID().toString();
        Instant firstClaim = Instant.now();

        assertThat(idempotencyPort.tryClaim(key, ENDPOINT, firstClaim, WINDOW)).isTrue();
        assertThat(idempotencyPort.tryClaim(key, ENDPOINT, firstClaim.plus(WINDOW).plusSeconds(1), WINDOW)).isTrue();
    }

    @Test
    void completingAnUnclaimedKeyFails() { // completar una clave no reclamada falla con el mismo mensaje de siempre
        assertThatThrownBy(() -> idempotencyPort.complete(UUID.randomUUID().toString(), ENDPOINT, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No idempotency key claimed for endpoint");
    }
}
