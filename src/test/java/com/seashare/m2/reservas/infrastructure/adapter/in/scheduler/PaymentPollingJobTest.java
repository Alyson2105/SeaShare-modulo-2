package com.seashare.m2.reservas.infrastructure.adapter.in.scheduler;

import com.seashare.m2.reservas.application.port.in.PollPaymentStatusUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPollingJobTest { // CU-13: el job solo delega y nunca propaga

    @Mock
    private PollPaymentStatusUseCase pollPaymentStatus;

    @Test
    void delegatesToTheUseCase() {
        when(pollPaymentStatus.execute()).thenReturn(2);
        PaymentPollingJob job = new PaymentPollingJob(pollPaymentStatus);

        job.run();

        verify(pollPaymentStatus).execute();
    }

    @Test
    void swallowsFailuresSoTheSchedulerKeepsRunning() {
        when(pollPaymentStatus.execute()).thenThrow(new IllegalStateException("boom"));
        PaymentPollingJob job = new PaymentPollingJob(pollPaymentStatus);

        assertThatCode(job::run).doesNotThrowAnyException();
    }
}
