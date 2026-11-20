package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ChangeReservationStatusCommand;
import com.seashare.m2.reservas.application.port.in.ChangeReservationStatusUseCase;
import com.seashare.m2.reservas.application.port.out.ReservationRepositoryPort;
import com.seashare.m2.reservas.domain.exception.InvalidTransitionException;
import com.seashare.m2.reservas.domain.exception.VersionConflictException;
import com.seashare.m2.reservas.domain.model.ActorType;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import com.seashare.m2.shared.domain.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpireReservationsServiceTest { // pruebas unitarias de ExpireReservationsService con puertos simulados y reloj fijo

    @Mock
    private ReservationRepositoryPort repository; // puerto de lectura simulado

    @Mock
    private ChangeReservationStatusUseCase changeStatus; // caso de uso simulado (CU-08)

    @Mock
    private ClockPort clockPort; // reloj fijo simulado

    private ExpireReservationsService service; // servicio bajo prueba

    private static final Instant FIXED_NOW = Instant.parse("2026-11-03T10:00:00Z"); // instante fijo para los tests
    private static final int BATCH_SIZE = 10; // tamano de lote para los tests

    @BeforeEach
    void setUp() { // inicializa el servicio con dependencias simuladas
        service = new ExpireReservationsService(repository, changeStatus, clockPort, BATCH_SIZE);
        when(clockPort.now()).thenReturn(FIXED_NOW); // el reloj siempre devuelve el instante fijo
    }

    @Test
    void expiraSoloLasVencidas() { // expira las reservas cuyo TTL ya vencio
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE)).thenReturn(List.of(id1, id2));

        int result = service.execute();

        assertThat(result).isEqualTo(2);
        ArgumentCaptor<ChangeReservationStatusCommand> captor = ArgumentCaptor.forClass(ChangeReservationStatusCommand.class);
        verify(changeStatus, times(2)).execute(captor.capture());
        assertThat(captor.getAllValues()).allMatch(cmd ->
                cmd.targetStatus() == ReservationStatus.EXPIRED
                        && cmd.actor() == ActorType.SYSTEM
                        && cmd.subtype() == null
        );
    }

    @Test
    void noProcesaNadaSiNoHayVencidas() { // no hace nada si la lista retornada por el repositorio esta vacia
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE)).thenReturn(List.of());

        int result = service.execute();

        assertThat(result).isZero();
        verify(changeStatus, never()).execute(any());
    }

    @Test
    void bordeExactoVenceEnNowExpira() { // borde exacto: si vence en now, es recuperada y expirada
        UUID id = UUID.randomUUID();
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE)).thenReturn(List.of(id));

        int result = service.execute();

        assertThat(result).isEqualTo(1);
        verify(changeStatus).execute(any());
    }

    @Test
    void bordeUnSegundoAntesNoExpira() { // si el TTL no ha vencido, el repositorio no la retorna y no se expira
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE)).thenReturn(List.of());

        int result = service.execute();

        assertThat(result).isZero();
        verify(changeStatus, never()).execute(any());
    }

    @Test
    void conflictoDeVersionSeOmiteSinError() { // VersionConflictException se omite y no propaga error
        UUID id = UUID.randomUUID();
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE)).thenReturn(List.of(id));
        doThrow(new VersionConflictException("conflicto")).when(changeStatus).execute(any());

        int result = service.execute(); // no debe lanzar excepcion

        assertThat(result).isZero(); // omision
    }

    @Test
    void transicionInvalidaSeOmiteSinError() { // InvalidTransitionException se omite y no propaga error
        UUID id = UUID.randomUUID();
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE)).thenReturn(List.of(id));
        doThrow(new InvalidTransitionException(ReservationStatus.EXPIRED, ReservationStatus.EXPIRED, "ya expirada"))
                .when(changeStatus).execute(any());

        int result = service.execute(); // no debe lanzar excepcion

        assertThat(result).isZero();
    }

    @Test
    void excepcionInesperadaEnUnaReservaNoImpideProcesarLasDemas() { // una excepcion no detiene el lote
        UUID idBueno1 = UUID.randomUUID();
        UUID idFallido = UUID.randomUUID();
        UUID idBueno2 = UUID.randomUUID();
        when(repository.findExpiredBefore(FIXED_NOW, BATCH_SIZE))
                .thenReturn(List.of(idBueno1, idFallido, idBueno2));

        org.mockito.Mockito.doAnswer(invocation -> {
            ChangeReservationStatusCommand cmd = invocation.getArgument(0);
            if (idFallido.equals(cmd.reservationId())) {
                throw new RuntimeException("error inesperado");
            }
            return null;
        }).when(changeStatus).execute(any());

        int result = service.execute();

        assertThat(result).isEqualTo(2);
        verify(changeStatus, times(3)).execute(any());
    }


    @Test
    void respetaLimiteDeLote() { // pasa el batchSize configurado al repositorio
        when(repository.findExpiredBefore(eq(FIXED_NOW), eq(BATCH_SIZE))).thenReturn(List.of());

        service.execute();

        verify(repository).findExpiredBefore(FIXED_NOW, BATCH_SIZE);
    }

    @Test
    void nuncaLlamaInstantNowDirectamente() { // usa siempre ClockPort
        when(repository.findExpiredBefore(any(Instant.class), anyInt())).thenReturn(List.of());

        service.execute();

        verify(clockPort).now();
    }
}
