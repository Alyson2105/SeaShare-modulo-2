package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.springframework.stereotype.Component;

/**
 * Matriz de notificaciones externas de CU-08 (T-08).
 *
 * <p>Fuente de verdad del mapeo hacia Modulo 1: tabla FR-002 de
 * {@code CU-22-recibir-informacion-estado-operativo/spec.md}. Fuente de verdad hacia Modulo 3:
 * FR-001 de {@code CU-14-recibir-estado-reserva/spec.md} (D-03).</p>
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
     * @param m1Blocked true si a M1 ya se le notifico RESERVED para esta reserva (guarda SC-003)
     */
    public NotificationDecision decide(ReservationStatus from,
                                       ReservationStatus to,
                                       Boolean mechanicalFailure,
                                       boolean m1Blocked) {
        boolean notifyM3 = notifiesModuleThree(to); // M3 (CU-14 FR-001)
        OperationalStatus m1Status = moduleOneStatus(to, mechanicalFailure, m1Blocked); // M1 (CU-22 FR-002)
        return new NotificationDecision(notifyM3, m1Status);
    }

    private boolean notifiesModuleThree(ReservationStatus to) { // estados que M3 debe conocer
        return switch (to) {
            case PENDING_PAYMENT, RESERVED, IN_NAVIGATION, COMPLETED, CANCELLED -> true; // desde Pendiente de Pago (D-03)
            case INITIATED, EXPIRED, PAYMENT_FAILED -> false; // no se publican (CU-14 FR-001)
        };
    }

    private OperationalStatus moduleOneStatus(ReservationStatus to, Boolean mechanicalFailure, boolean m1Blocked) {
        return switch (to) { // tabla FR-002 de CU-22
            case RESERVED -> OperationalStatus.RESERVED; // unico momento del bloqueo; el servicio pone m1_blocked=true
            case IN_NAVIGATION -> OperationalStatus.IN_NAVIGATION; // embarcacion en uso
            case COMPLETED -> m1Blocked ? OperationalStatus.MAINTENANCE_CLEANING : null; // senal de mantenimiento solo si hubo bloqueo previo
            case CANCELLED -> { // condicional: solo si M1 ya la tenia marcada como ocupada
                if (!m1Blocked) {
                    yield null; // cancelacion antes de RESERVED: nunca hubo bloqueo que liberar
                }
                yield mechanicalFailure != null && mechanicalFailure
                        ? OperationalStatus.MAINTENANCE_CLEANING // averia del Propietario
                        : OperationalStatus.AVAILABLE; // liberacion ordinaria
            }
            case INITIATED, PENDING_PAYMENT, EXPIRED, PAYMENT_FAILED -> null; // nunca se notifica a M1
        };
    }
}
