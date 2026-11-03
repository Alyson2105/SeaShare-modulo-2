package com.seashare.m2.reservas.infrastructure.adapter.in;

import com.seashare.m2.reservas.application.port.in.ExpireReservationsUseCase;
import com.seashare.m2.reservas.infrastructure.adapter.in.scheduler.TtlExpiryJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TtlExpiryJobTest { // prueba que TtlExpiryJob delega en ExpireReservationsUseCase sin lógica de negocio propia

    @Mock
    private ExpireReservationsUseCase expireReservationsUseCase; // caso de uso simulado

    @InjectMocks
    private TtlExpiryJob job; // job bajo prueba

    @Test
    void runDelegaEnElCasoDeUso() { // el job solo llama al caso de uso, sin lógica adicional
        when(expireReservationsUseCase.execute()).thenReturn(3); // simula 3 reservas expiradas

        job.run(); // ejecuta el job

        verify(expireReservationsUseCase).execute(); // caso de uso fue invocado exactamente una vez
    }
}
