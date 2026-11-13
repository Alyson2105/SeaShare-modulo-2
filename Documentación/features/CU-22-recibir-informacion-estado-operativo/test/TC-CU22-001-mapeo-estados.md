### Test Case Specification: TC-CU22-001 — Mapeo operativo
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU22-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar mapeo operativo. Requisitos: FR-001, FR-002. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-22 encola destino M1_HTTP; el adaptador actual es fake y no realiza HTTP real. La matriz es local; adaptador de notificación real a M1 aún no existe.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas preparadas para transiciones notificables, con guarda m1_blocked.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Transicionar a RESERVED, IN_NAVIGATION y COMPLETED e inspeccionar outbox. | Datos de las precondiciones | Se encolan respectivamente RESERVED, IN_NAVIGATION y MAINTENANCE_CLEANING como estados operativos. | Pendiente | ChangeReservationStatusServiceTest.java);; ModuleNotificationMatrixTest.java); ModuleNotificationMatrixTest#reservedNotifiesModuleOneWithReserved; ModuleNotificationMatrixTest#inNavigationAndCompletedNotifyModuleOne: exitosa. Pruebas unitarias verifican mapeo de RESERVED, IN_NAVIGATION y COMPLETED; no prueban entrega HTTP real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La matriz es local; adaptador de notificación real a M1 aún no existe.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial; Pruebas unitarias verifican mapeo de RESERVED, IN_NAVIGATION y COMPLETED; no prueban entrega HTTP real.

#### Traceability
FR-001, FR-002 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java)  | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#reservedNotifiesModuleOneWithReserved | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#inNavigationAndCompletedNotifyModuleOne | In Process
