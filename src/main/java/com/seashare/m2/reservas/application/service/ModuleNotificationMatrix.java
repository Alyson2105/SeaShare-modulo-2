package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.springframework.stereotype.Component;

/**
 * Matriz de notificaciones externas de CU-08 (T-08).
 *
 * <p>Fuente de verdad del mapeo hacia Modulo 1: contrato {@code consistencia-m2-m1.md} §8
 * (FR-002 sin {@code RESERVED}). Fuente de verdad hacia Modulo 3: FR-001 de
 * {@code CU-14-recibir-estado-reserva/spec.md} (D-03).</p>
 */
@Component
public class ModuleNotificationMatrix { // componente puro: decide que destinos notifican cada transicion

    /** Resultado de la decision para una transicion. */
    public record NotificationDecision(boolean notifyModuleThree, OperationalStatus moduleOneStatus) {
        public static final NotificationDecision NONE = new NotificationDecision(false, null); // nadie notifica
    }

    /**
     * Decide las notificaciones de una transicion consolidada.
     *
     * @param from estado anterior
     * @param to estado destino
     * @param mechanicalFailure true si la cancelacion asocia averia mecanica reportada por el Propietario
     * @param m1Blocked true si M1 ya conoce la ocupacion (se le envio IN_NAVIGATION)
     * @param maintenanceBlocked true si la reserva se cancela por un mantenimiento activo
     */
    public NotificationDecision decide(ReservationStatus from,
                                       ReservationStatus to,
                                       Boolean mechanicalFailure,
                                       boolean m1Blocked,
                                       boolean maintenanceBlocked) {
        boolean notifyM3 = notifiesModuleThree(to); // M3 (CU-14 FR-001)
        OperationalStatus m1Status = moduleOneStatus(to, mechanicalFailure, m1Blocked, maintenanceBlocked); // M1 (contrato C4)
        return new NotificationDecision(notifyM3, m1Status);
    }

    private boolean notifiesModuleThree(ReservationStatus to) { // estados que M3 debe conocer
        return switch (to) {
            case PENDING_PAYMENT, RESERVED, IN_NAVIGATION, COMPLETED, CANCELLED -> true; // desde Pendiente de Pago (D-03)
            case INITIATED, EXPIRED, PAYMENT_FAILED -> false; // no se publican (CU-14 FR-001)
        };
    }

    private OperationalStatus moduleOneStatus(ReservationStatus to,
                                              Boolean mechanicalFailure,
                                              boolean m1Blocked,
                                              boolean maintenanceBlocked) {
        return switch (to) { // tabla del contrato M1-M2 §8
            case RESERVED -> null; // M1 deja de recibir RESERVED: el bloqueo por fechas es de M2
            case IN_NAVIGATION -> OperationalStatus.IN_NAVIGATION; // check-in: M1 conoce la ocupacion
            case COMPLETED -> m1Blocked ? OperationalStatus.MAINTENANCE_CLEANING : null; // solo si M1 conocia la ocupacion
            case CANCELLED -> { // liberacion condicional
                if (!m1Blocked || maintenanceBlocked) {
                    yield null; // sin ocupacion conocida, o el barco sigue en mantenimiento
                }
                yield mechanicalFailure != null && mechanicalFailure
                        ? OperationalStatus.MAINTENANCE_CLEANING // averia del Propietario
                        : OperationalStatus.AVAILABLE; // liberacion ordinaria
            }
            case INITIATED, PENDING_PAYMENT, EXPIRED, PAYMENT_FAILED -> null; // nunca se notifica a M1
        };
    }
}
