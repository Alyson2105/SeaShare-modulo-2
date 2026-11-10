package com.seashare.m2.reservas.infrastructure.adapter.out.messaging;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceStatusMapperTest { // pruebas del mapeo de estados internos al contrato UC07 de M3

    @Test
    void mapsPendingPaymentToPending() { // PENDING_PAYMENT se publica como PENDING
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.PENDING_PAYMENT, null)).isEqualTo("PENDING");
    }

    @Test
    void mapsReservedToReserved() { // RESERVED conserva su nombre
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.RESERVED, null)).isEqualTo("RESERVED");
    }

    @Test
    void mapsInNavigationToInNavigation() { // IN_NAVIGATION conserva su nombre
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.IN_NAVIGATION, null)).isEqualTo("IN_NAVIGATION");
    }

    @Test
    void mapsCompletedToCompleted() { // COMPLETED conserva su nombre
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.COMPLETED, null)).isEqualTo("COMPLETED");
    }

    @Test
    void mapsCancelledFlexibleToCancelledFlexible() { // FLEXIBLE se publica como CANCELLED_FLEXIBLE
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE))
                .isEqualTo("CANCELLED_FLEXIBLE");
    }

    @Test
    void mapsCancelledModerateToCancelledModerate() { // MODERATE se publica como CANCELLED_MODERATE
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.CANCELLED, CancellationSubtype.MODERATE))
                .isEqualTo("CANCELLED_MODERATE");
    }

    @Test
    void mapsCancelledLateToCancelledStrict() { // LATE se publica como CANCELLED_STRICT
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.CANCELLED, CancellationSubtype.LATE))
                .isEqualTo("CANCELLED_STRICT");
    }

    @Test
    void mapsCancelledNoShowToCancelledStrict() { // NO_SHOW se publica como CANCELLED_STRICT
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW))
                .isEqualTo("CANCELLED_STRICT");
    }

    @Test
    void mapsCancelledByOwnerToCancelledByHost() { // BY_OWNER se publica como CANCELLED_BY_HOST
        assertThat(FinanceStatusMapper.toFinanceStatus(ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER))
                .isEqualTo("CANCELLED_BY_HOST");
    }

    @Test
    void throwsWhenCancelledWithoutSubtype() { // CANCELLED exige subtipo para publicarse
        assertThatThrownBy(() -> FinanceStatusMapper.toFinanceStatus(ReservationStatus.CANCELLED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cancellation subtype is required to publish CANCELLED");
    }

    @Test
    void throwsForInitiated() { // INITIATED nunca se publica a M3
        assertThatThrownBy(() -> FinanceStatusMapper.toFinanceStatus(ReservationStatus.INITIATED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Status INITIATED must not be published to Module 3");
    }

    @Test
    void throwsForExpired() { // EXPIRED nunca se publica a M3
        assertThatThrownBy(() -> FinanceStatusMapper.toFinanceStatus(ReservationStatus.EXPIRED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Status EXPIRED must not be published to Module 3");
    }

    @Test
    void throwsForPaymentFailed() { // PAYMENT_FAILED nunca se publica a M3
        assertThatThrownBy(() -> FinanceStatusMapper.toFinanceStatus(ReservationStatus.PAYMENT_FAILED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Status PAYMENT_FAILED must not be published to Module 3");
    }
}
