package com.seashare.m2.disputa.infrastructure.adapter.in.scheduler;

import com.seashare.m2.disputa.application.port.in.CloseExpiredDisputesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeWindowCloseJobTest { // el job solo delega y nunca propaga excepciones

    @Mock
    private CloseExpiredDisputesUseCase closeExpiredDisputesUseCase; // caso de uso simulado

    @InjectMocks
    private DisputeWindowCloseJob job; // job bajo prueba

    @Test
    void runDelegatesToUseCase() { // sin logica adicional
        when(closeExpiredDisputesUseCase.execute()).thenReturn(2);

        job.run();

        verify(closeExpiredDisputesUseCase).execute();
    }

    @Test
    void runNeverPropagatesExceptions() { // el scheduler sigue vivo
        when(closeExpiredDisputesUseCase.execute()).thenThrow(new RuntimeException("base caida"));

        assertThatCode(job::run).doesNotThrowAnyException();
    }
}
