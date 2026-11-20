package com.seashare.m2.reservas.infrastructure.adapter.in.web;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusBadgeTest { // CU-20 FR-008 y CU-21 FR-015: etiquetas y colores por estado

    @Test
    void everyStatusHasABadge() { // ningun estado queda sin metadatos visuales
        for (ReservationStatus status : ReservationStatus.values()) { // recorre el catalogo completo
            StatusBadge badge = StatusBadge.from(status, null); // estado sin subtipo
            assertThat(badge.text()).as("texto de %s", status).isNotBlank(); // texto legible
            assertThat(badge.semanticColor()).as("color de %s", status).isNotBlank(); // color semantico
            assertThat(badge.description()).as("descripcion de %s", status).isNotBlank(); // descripcion
        }
    }

    @Test
    void reservedIsGreenAndConfirmed() { // caso de referencia del plan de CU-20
        StatusBadge badge = StatusBadge.from(ReservationStatus.RESERVED, null);

        assertThat(badge.text()).isEqualTo("Confirmada");
        assertThat(badge.semanticColor()).isEqualTo("verde");
    }

    @Test
    void inNavigationIsYellow() { // viaje en curso
        assertThat(StatusBadge.from(ReservationStatus.IN_NAVIGATION, null).semanticColor())
                .isEqualTo("amarillo");
    }

    @Test
    void completedIsGray() { // viaje cerrado sin conflicto
        assertThat(StatusBadge.from(ReservationStatus.COMPLETED, null).semanticColor())
                .isEqualTo("gris");
    }

    @Test
    void expiredIsGrayAndPaymentFailedIsRed() { // cierre pasivo frente a fallo de pago
        assertThat(StatusBadge.from(ReservationStatus.EXPIRED, null).semanticColor()).isEqualTo("gris");
        assertThat(StatusBadge.from(ReservationStatus.PAYMENT_FAILED, null).semanticColor()).isEqualTo("rojo");
    }

    @Test
    void cancellationSubtypesHaveContextualText() { // cada subestado explica su politica
        assertThat(StatusBadge.from(ReservationStatus.CANCELLED, CancellationSubtype.FLEXIBLE).description())
                .contains("> 72 h");
        assertThat(StatusBadge.from(ReservationStatus.CANCELLED, CancellationSubtype.MODERATE).description())
                .contains("72 y 24 h");
        assertThat(StatusBadge.from(ReservationStatus.CANCELLED, CancellationSubtype.LATE).description())
                .contains("< 24 h");
        assertThat(StatusBadge.from(ReservationStatus.CANCELLED, CancellationSubtype.BY_OWNER).text())
                .isEqualTo("Cancelada por el propietario");
        assertThat(StatusBadge.from(ReservationStatus.CANCELLED, CancellationSubtype.NO_SHOW).text())
                .isEqualTo("Cancelado por inasistencia");
    }

    @Test
    void cancellationWithoutSubtypeIsGeneric() { // cancelacion sin subestado registrado
        StatusBadge badge = StatusBadge.from(ReservationStatus.CANCELLED, null);

        assertThat(badge.text()).isEqualTo("Cancelada");
        assertThat(badge.semanticColor()).isEqualTo("rojo");
    }
}
