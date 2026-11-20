package com.seashare.m2.reservas.application.service;

import com.seashare.m2.reservas.domain.model.OperationalStatus;
import com.seashare.m2.reservas.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleOneNotificationMatrixTest { // pruebas unitarias de la matriz de notificaciones (contrato M1-M2 §8, CU-14 FR-001)

    private final ModuleNotificationMatrix matrix = new ModuleNotificationMatrix(); // componente bajo prueba

    @Test
    void initiatedAndPendingPaymentNotifyNobody() { // INITIATED y PENDING_PAYMENT: sin M1 (y INITIATED sin M3)
        ModuleNotificationMatrix.NotificationDecision initiated =
                matrix.decide(ReservationStatus.INITIATED, ReservationStatus.INITIATED, null, false, false);
        assertThat(initiated.notifyModuleThree()).isFalse(); // creacion sin evento de estado
        assertThat(initiated.moduleOneStatus()).isNull(); // nunca se notifica a M1

        ModuleNotificationMatrix.NotificationDecision pending =
                matrix.decide(ReservationStatus.INITIATED, ReservationStatus.PENDING_PAYMENT, null, false, false);
        assertThat(pending.notifyModuleThree()).isTrue(); // D-03: si se publica a M3
        assertThat(pending.moduleOneStatus()).isNull(); // M1 no participa de PENDIENTE DE PAGO
    }

    @Test
    void reservedDoesNotNotifyModuleOne() { // RESERVED ya no se envia a M1
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.PENDING_PAYMENT, ReservationStatus.RESERVED, null, false, false);
        assertThat(decision.notifyModuleThree()).isTrue();
        assertThat(decision.moduleOneStatus()).isNull(); // M1 no recibe RESERVED (contrato C4)
    }

    @Test
    void inNavigationAndCompletedNotifyModuleOne() { // check-in y cierre notifican a M1
        assertThat(matrix.decide(ReservationStatus.RESERVED, ReservationStatus.IN_NAVIGATION, false, false, false)
                .moduleOneStatus()).isEqualTo(OperationalStatus.IN_NAVIGATION); // siempre en el check-in
        assertThat(matrix.decide(ReservationStatus.IN_NAVIGATION, ReservationStatus.COMPLETED, false, true, false)
                .moduleOneStatus()).isEqualTo(OperationalStatus.MAINTENANCE_CLEANING); // senal de limpieza
    }

    @Test
    void completedWithoutOccupancyKnownDoesNotNotifyModuleOne() { // sin check-in previo M1 no conoce la ocupacion
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.IN_NAVIGATION, ReservationStatus.COMPLETED, false, false, false);
        assertThat(decision.moduleOneStatus()).isNull();
    }

    @Test
    void cancelledBeforeInNavigationDoesNotNotifyModuleOne() { // la guarda vive en IN_NAVIGATION, no en RESERVED
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, false, false, false);
        assertThat(decision.moduleOneStatus()).isNull(); // nunca se aviso la ocupacion
        assertThat(decision.notifyModuleThree()).isTrue();
    }

    @Test
    void cancelledAfterInNavigationNotifiesAvailable() { // liberacion ordinaria cuando M1 si conocia la ocupacion
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, false, true, false);
        assertThat(decision.moduleOneStatus()).isEqualTo(OperationalStatus.AVAILABLE);
        assertThat(decision.notifyModuleThree()).isTrue(); // M3 si recibe la cancelacion
    }

    @Test
    void cancelledByOwnerWithMechanicalFailureNotifiesMaintenance() { // averia del Propietario -> mantenimiento
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, true, true, false);
        assertThat(decision.moduleOneStatus()).isEqualTo(OperationalStatus.MAINTENANCE_CLEANING);
    }

    @Test
    void cancelledByMaintenanceDoesNotReleaseVessel() { // una cancelacion por mantenimiento no envia AVAILABLE
        ModuleNotificationMatrix.NotificationDecision decision =
                matrix.decide(ReservationStatus.RESERVED, ReservationStatus.CANCELLED, false, true, true);
        assertThat(decision.moduleOneStatus()).isNull();
        assertThat(decision.notifyModuleThree()).isTrue(); // M3 si recibe CANCELLED_BY_HOST
    }

    @Test
    void expiredAndPaymentFailedNotifyNobody() { // EXPIRED y PAYMENT_FAILED: nadie notifica
        for (ReservationStatus origin : new ReservationStatus[]{ReservationStatus.INITIATED, ReservationStatus.PENDING_PAYMENT}) {
            ModuleNotificationMatrix.NotificationDecision expired =
                    matrix.decide(origin, ReservationStatus.EXPIRED, null, origin == ReservationStatus.PENDING_PAYMENT, false);
            assertThat(expired.notifyModuleThree()).isFalse(); // no se publican a M3 (CU-14 FR-001)
            assertThat(expired.moduleOneStatus()).isNull(); // no se notifican a M1
        }
        ModuleNotificationMatrix.NotificationDecision failed =
                matrix.decide(ReservationStatus.PENDING_PAYMENT, ReservationStatus.PAYMENT_FAILED, null, false, false);
        assertThat(failed.notifyModuleThree()).isFalse();
        assertThat(failed.moduleOneStatus()).isNull();
    }
}
