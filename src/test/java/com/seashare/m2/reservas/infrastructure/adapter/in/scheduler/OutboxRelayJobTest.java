package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.port.in.PublishPendingOutboxUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayJobTest { // prueba que OutboxRelayJob solo delega y nunca propaga excepciones

    @Mock
    private PublishPendingOutboxUseCase publishPendingOutboxUseCase; // caso de uso simulado

    @InjectMocks
    private OutboxRelayJob job; // job bajo prueba

    @Test
    void runDelegaEnElCasoDeUso() { // el job solo llama al caso de uso, sin logica adicional
        when(publishPendingOutboxUseCase.execute()).thenReturn(3); // simula 3 mensajes procesados

        job.run(); // ejecuta el job

        verify(publishPendingOutboxUseCase).execute(); // caso de uso invocado exactamente una vez
    }

    @Test
    void runNoPropagaExcepcionesDelCasoDeUso() { // cualquier fallo queda en el log, jamas escapa
        when(publishPendingOutboxUseCase.execute()).thenThrow(new RuntimeException("broker caido")); // fallo simulado

        assertThatCode(job::run) // el job no debe lanzar
                .doesNotThrowAnyException(); // el scheduler sigue vivo
    }
}
