package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.application.dto.ReservationDetailView;
import com.seashare.m2.reservas.application.dto.ReservationView;
import com.seashare.m2.reservas.domain.model.CancellationSubtype;
import com.seashare.m2.reservas.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

final class ReservationDetailPolicy { // reglas de presentacion del detalle de CU-21, sin aritmetica monetaria

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm"); // hora local del puerto

    private static final long NO_SHOW_TOLERANCE_MINUTES = 30; // tolerancia fija de CU-21 FR-018

    private static final Duration DISPUTE_WINDOW = Duration.ofHours(24); // ventana de disputa de garantia

    private ReservationDetailPolicy() { // clase de utilidades
    }

    static ReservationDetailView.PaymentSummary paymentSummary(ReservationView view) { // adapta el desglose al estado
        BigDecimal rental = view.rentalAmount(); // alquiler persistido
        BigDecimal insurance = view.insuranceAmount(); // seguro persistido
        BigDecimal deposit = view.guaranteeDepositAmount(); // deposito persistido
        String title = "Resumen de pago"; // titulo por defecto
        String note = null; // sin nota por defecto

        if (view.status() == ReservationStatus.COMPLETED) { // ventana de disputa: solo deposito
            title = "Garantía en custodia"; // titulo del plan
            rental = null; // se oculta el alquiler (FR-013)
            insurance = null; // se oculta el seguro (FR-013)
            note = "Depósito en custodia durante la ventana de disputa (24 h); la liquidación la gestiona Módulo 3.";
        } else if (view.status() == ReservationStatus.CANCELLED) { // cancelada: no se muestran rubros
            rental = null; // sin alquiler
            insurance = null; // sin seguro
            deposit = null; // sin deposito
            if (view.cancellationSubtype() == CancellationSubtype.NO_SHOW) { // compensacion por inasistencia
                title = "Compensación"; // FR-014
                note = "El sistema de pagos gestionará la compensación al propietario.";
            } else {
                note = "La liquidación y el reembolso los gestiona Módulo 3.";
            }
        }

        BigDecimal totalPaid = view.totalAmount(); // solo lo liquidado; nunca un estimado presentado como pagado
        return new ReservationDetailView.PaymentSummary(title, view.currency(), rental, insurance, deposit, totalPaid, note);
    }

    static ReservationDetailView.AvailableActions actions(ReservationView view,
                                                          String role,
                                                          OffsetDateTime scheduledDeparture,
                                                          Instant now) { // calcula flags de accion por rol y estado
        boolean owner = "OWNER".equalsIgnoreCase(role); // rol propietario
        Instant departure = scheduledDeparture.toInstant(); // zarpe pactado en instante
        Instant noShowEnabledAt = departure.plus(Duration.ofMinutes(NO_SHOW_TOLERANCE_MINUTES)); // fin de la tolerancia
        return new ReservationDetailView.AvailableActions(
                view.status() == ReservationStatus.RESERVED, // cualquier parte puede cancelar en RESERVED
                view.status() == ReservationStatus.INITIATED && !owner, // solo el arrendatario inicia pago
                owner && view.status() == ReservationStatus.RESERVED && !now.isBefore(departure), // inicio tras la hora pactada
                owner && view.status() == ReservationStatus.RESERVED && !now.isBefore(noShowEnabledAt), // inasistencia tras 30 min
                owner && view.status() == ReservationStatus.IN_NAVIGATION); // fin solo en navegacion
    }

    static ReservationDetailView.TimeIndicators timeIndicators(ReservationView view,
                                                               OffsetDateTime scheduledDeparture,
                                                               Instant now) { // calcula contadores no monetarios
        Long ttl = null; // segundos restantes de TTL
        if ((view.status() == ReservationStatus.INITIATED || view.status() == ReservationStatus.PENDING_PAYMENT)
                && view.ttlExpiresAt() != null) {
            ttl = Math.max(0, Duration.between(now, view.ttlExpiresAt()).getSeconds()); // nunca negativo
        }

        Long minutesToDeparture = null; // minutos hasta el zarpe
        Long courtesyElapsed = null; // minutos de cortesia transcurridos
        Instant departure = scheduledDeparture.toInstant(); // zarpe pactado
        if (view.status() == ReservationStatus.RESERVED) { // reserva previa al viaje
            minutesToDeparture = Math.max(0, Duration.between(now, departure).toMinutes()); // cero si ya paso
            if (!now.isBefore(departure)) { // ya paso la hora pactada
                courtesyElapsed = Math.max(0, Duration.between(departure, now).toMinutes()); // cortesia en curso
            }
        } else if (view.status() != ReservationStatus.INITIATED && view.status() != ReservationStatus.PENDING_PAYMENT) {
            minutesToDeparture = 0L; // estados posteriores muestran cero
        }

        Long disputeWindow = null; // segundos restantes de disputa
        if (view.status() == ReservationStatus.COMPLETED) { // ventana de 24 h
            Instant base = view.actualArrivalAt() != null ? view.actualArrivalAt() : view.statusChangedAt(); // llegada real o cierre
            if (base != null) {
                disputeWindow = Math.max(0, DISPUTE_WINDOW.getSeconds() - Duration.between(base, now).getSeconds());
            }
        }

        return new ReservationDetailView.TimeIndicators(ttl, minutesToDeparture, courtesyElapsed, disputeWindow);
    }

    static ReservationDetailView.Banner banner(ReservationView view, OffsetDateTime actualDeparture) { // banner contextual por estado
        return switch (view.status()) {
            case INITIATED -> new ReservationDetailView.Banner("informative", "Reserva iniciada",
                    "Completa el pago antes de que expire la reserva.");
            case PENDING_PAYMENT -> new ReservationDetailView.Banner("informative", "Pago pendiente",
                    "Estamos confirmando el pago de la reserva.");
            case RESERVED -> new ReservationDetailView.Banner("informative", "Reserva confirmada",
                    "La embarcación estará alistada para el zarpe pactado.");
            case IN_NAVIGATION -> new ReservationDetailView.Banner("active", "El viaje está en curso",
                    actualDeparture == null
                            ? "Navegación iniciada."
                            : "Navegación iniciada a las " + actualDeparture.format(TIME_FORMATTER) + ".");
            case COMPLETED -> new ReservationDetailView.Banner("informative", "Viaje completado",
                    "La embarcación ha sido devuelta y atracada.");
            case CANCELLED -> view.cancellationSubtype() == CancellationSubtype.NO_SHOW
                    ? new ReservationDetailView.Banner("alert", "Reserva cancelada por inasistencia",
                            "Se cumplió la ventana de cortesía de 30 minutos sin presentación del cliente.")
                    : new ReservationDetailView.Banner("alert", "Reserva cancelada",
                            "La reserva fue cancelada; la liquidación la gestiona Módulo 3.");
            case EXPIRED -> new ReservationDetailView.Banner("alert", "Reserva expirada",
                    "El tiempo para completar el pago venció.");
            case PAYMENT_FAILED -> new ReservationDetailView.Banner("alert", "Pago fallido",
                    "No se pudo confirmar el pago de la reserva.");
        };
    }
}
