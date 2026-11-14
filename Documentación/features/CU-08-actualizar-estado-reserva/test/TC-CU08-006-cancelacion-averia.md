### Test Case Specification: TC-CU08-006 — Notificación por cancelación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar notificación por cancelación. Requisitos: FR-003, FR-007. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Las variantes sin avería y sin guarda no se demuestran conjuntamente en flujo integrado.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva RESERVED con m1_blocked verdadero; preparar cancelaciones independientes con y sin avería.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ChangeReservationStatusServiceTest#cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance (XML V4: passed). Captura mediante espía el evento operativo y aserta MAINTENANCE_CLEANING. Las variantes sin avería y sin guarda no se demuestran conjuntamente en flujo integrado. |
| 2 | Ejecutar | Cancelar con actor y subtipo válidos. | Datos de las precondiciones | Sin avería se encola AVAILABLE; avería del propietario encola MAINTENANCE_CLEANING; sin guarda no se libera M1. | Pendiente | ChangeReservationStatusServiceTest#cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance (XML V4: passed). Captura mediante espía el evento operativo y aserta MAINTENANCE_CLEANING. Las variantes sin avería y sin guarda no se demuestran conjuntamente en flujo integrado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ChangeReservationStatusServiceTest#cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance (XML V4: passed). Captura mediante espía el evento operativo y aserta MAINTENANCE_CLEANING. Las variantes sin avería y sin guarda no se demuestran conjuntamente en flujo integrado. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Los destinos son decisiones del motor, no entrega confirmada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ChangeReservationStatusServiceTest#cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance. Las variantes sin avería y sin guarda no se demuestran conjuntamente en flujo integrado.

#### Traceability
FR-003, FR-007 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#cancelledByOwnerWithMechanicalFailureEnqueuesMaintenance; src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledAfterReservedNotifiesAvailable; src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledBeforeReservedDoesNotNotifyModuleOne | In Process
