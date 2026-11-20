package com.seashare.m2.reservas.infrastructure.adapter.out.messaging;

import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

public final class FinanceStatusMapper { // traduce nombres internos a los 10 valores de UC07 de M3 (P17)

    private FinanceStatusMapper() { // clase utilitaria: sin instancias
    }

    public static String toFinanceStatus(ReservationStatus status, CancellationSubtype subtype) { // mapea estado y subtipo al nombre que espera M3
        return switch (status) {
            case PENDING_PAYMENT -> "PENDING"; // pago en curso
            case RESERVED -> "RESERVED"; // pago confirmado
            case IN_NAVIGATION -> "IN_NAVIGATION"; // navegacion en curso
            case COMPLETED -> "COMPLETED"; // navegacion terminada
            case CANCELLED -> cancelled(subtype); // cancelacion: el nombre depende del subtipo
            case INITIATED, EXPIRED, PAYMENT_FAILED -> throw new IllegalStateException(
                    "Status " + status + " must not be published to Module 3"); // estados fuera del contrato UC07
        };
    }

    private static String cancelled(CancellationSubtype subtype) { // traduce el subtipo de cancelacion
        if (subtype == null) { // sin subtipo no se puede nombrar la cancelacion
            throw new IllegalStateException("Cancellation subtype is required to publish CANCELLED");
        }
        return switch (subtype) {
            case FLEXIBLE -> "CANCELLED_FLEXIBLE"; // politica flexible
            case MODERATE -> "CANCELLED_MODERATE"; // politica moderada
            case LATE, NO_SHOW -> "CANCELLED_STRICT"; // tardia o inasistencia
            case BY_OWNER -> "CANCELLED_BY_HOST"; // cancelada por el propietario
        };
    }
}
