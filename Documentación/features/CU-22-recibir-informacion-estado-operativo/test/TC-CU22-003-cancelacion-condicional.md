### Test Case Specification: TC-CU22-003 — Cancelación con guarda M1
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU22-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar cancelación con guarda m1. Requisitos: FR-001, FR-002. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-22 encola destino M1_HTTP; el adaptador actual es fake y no realiza HTTP real. La cancelación activa solo se admite desde RESERVED; la matriz pura puede evaluar otras combinaciones.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas RESERVED con m1_blocked y causas ordinaria y avería mecánica.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Cancelar en escenarios independientes. | Datos de las precondiciones | Se encola AVAILABLE para cancelación ordinaria y MAINTENANCE_CLEANING por avería del propietario; sin guarda no se libera. | Pendiente | ChangeReservationStatusServiceTest.java);; ModuleNotificationMatrixTest.java); ModuleNotificationMatrixTest#cancelledAfterReservedNotifiesAvailable; ModuleNotificationMatrixTest#cancelledByOwnerWithMechanicalFailureNotifiesMaintenance; ModuleNotificationMatrixTest#cancelledBeforeReservedDoesNotNotifyModuleOne: exitosa. Matriz unitaria comprueba guarda y mapeos de cancelación; no prueba flujo completo de cancelación. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La cancelación activa solo se admite desde RESERVED; la matriz pura puede evaluar otras combinaciones.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial; Matriz unitaria comprueba guarda y mapeos de cancelación; no prueba flujo completo de cancelación.

#### Traceability
FR-001, FR-002 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java)  | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledAfterReservedNotifiesAvailable | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledByOwnerWithMechanicalFailureNotifiesMaintenance | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledBeforeReservedDoesNotNotifyModuleOne | In Process
