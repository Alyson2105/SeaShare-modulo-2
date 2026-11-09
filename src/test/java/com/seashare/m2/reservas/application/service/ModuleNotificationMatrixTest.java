package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleOneNotificationMatrixTest { // pruebas unitarias de la matriz de notificaciones (CU-22 FR-002, CU-14 FR-001)

    private final ModuleNotificationMatrix matrix = new ModuleNotificationMatrix(); // componente bajo prueba

    @Test
    void initiatedAndPendingPaymentNotifyNobody() { // INITIATED y PENDING_PAYMENT: sin M1 (y INITIATED sin M3)
        ModuleNotificationMatrix.NotificationDecision initiated =
                matrix.decide(ReservationStatus.INITIATED, ReservationStatus.INITIATED, null, false);
        assertThat(initiated.notifyModuleThree()).isFalse(); // creacion sin evento de estado
        assertThat(initiated.moduleOneStatus()).isNull(); // nunca se notifica a M1

        ModuleNotificationMatrix.NotificationDecision pending =
                matrix.decide(ReservationStatus.INITIATED, ReservationStatus.PENDING_PAYMENT, null, false);
        assertThat(pending.notifyModuleThree()).isTrue(); // D-03: si se publica a M3
        assertThat(pending.moduleOneStatus()).isNull(); // M1 no participa de PENDIENTE DE PAGO
    }

    @Test
    void reservedNotifiesModuleOneWithReserved() { // RESERVED bloquea en M1 una unica vez
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.PENDING_PAYMENT, ReservationStatus.RESERVED, null, false);
        assertThat(decision.notifyModuleThree()).isTrue();
        assertThat(decision.moduleOneStatus()).isEqualTo(OperationalStatus.RESERVED);
    }

    @Test
    void inNavigationAndCompletedNotifyModuleOne() { // check-in y cierre notifican a M1
        assertThat(matrix.decide(ReservationStatus.RESERVED, ReservationStatus.IN_NAVIGATION, null, true).moduleOneStatus())
                .isEqualTo(OperationalStatus.IN_NAVIGATION);
        assertThat(matrix.decide(ReservationStatus.IN_NAVIGATION, ReservationStatus.COMPLETED, null, true).moduleOneStatus())
                .isEqualTo(OperationalStatus.MAINTENANCE_CLEANING); // senal de mantenimiento/limpieza
    }

    @Test
    void cancelledBeforeReservedDoesNotNotifyModuleOne() { // guarda SC-003: sin bloqueo previo no hay liberacion
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CANCELLED, false, false);
        assertThat(decision.moduleOneStatus()).isNull(); // nunca llego a RESERVED -> no se notifica
    }

    @Test
    void cancelledAfterReservedNotifiesAvailable() { // liberacion ordinaria cuando si hubo bloqueo
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, false, true);
        assertThat(decision.moduleOneStatus()).isEqualTo(OperationalStatus.AVAILABLE);
        assertThat(decision.notifyModuleThree()).isTrue(); // M3 si recibe la cancelacion
    }

    @Test
    void cancelledByOwnerWithMechanicalFailureNotifiesMaintenance() { // averia del Propietario -> mantenimiento
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, true, true);
        assertThat(decision.moduleOneStatus()).isEqualTo(OperationalStatus.MAINTENANCE_CLEANING);
    }

    @Test
    void expiredAndPaymentFailedNotifyNobody() { // EXPIRED y PAYMENT_FAILED: nadie notifica
        for (ReservationStatus origin : new ReservationStatus[]{ReservationStatus.INITIATED, ReservationStatus.PENDING_PAYMENT}) {
            ModuleNotificationMatrix.NotificationDecision expired =
                    matrix.decide(origin, ReservationStatus.EXPIRED, null, origin == ReservationStatus.PENDING_PAYMENT);
            assertThat(expired.notifyModuleThree()).isFalse(); // no se publican a M3 (CU-14 FR-001)
            assertThat(expired.moduleOneStatus()).isNull(); // no se notifican a M1 (CU-22 FR-002)
        }
        ModuleNotificationMatrix.NotificationDecision failed =
                matrix.decide(ReservationStatus.PENDING_PAYMENT, ReservationStatus.PAYMENT_FAILED, null, false);
        assertThat(failed.notifyModuleThree()).isFalse();
        assertThat(failed.moduleOneStatus()).isNull();
    }
}
