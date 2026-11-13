### Test Case Specification: TC-CU05-003 — Rechazo por estado incompatible
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo por estado incompatible. Requisitos: FR-001, FR-006. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Prueba cuatro estados con mocks; no prueba todos los estados de la ficha. El endpoint y servicio existen. La expiración pasiva corresponde a CU-08.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-05.

##### Preconditions & Default Setup
Preparar reservas en INITIATED, PENDING_PAYMENT, PAYMENT_FAILED, IN_NAVIGATION, COMPLETED, EXPIRED y CANCELLED.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReportNoShowServiceTest#invalidStatesAreRejectedWithoutClockOrWrites: verde. Prueba cuatro estados con mocks; no prueba todos los estados de la ficha. |
| 2 | Ejecutar | Intentar reportar por separado cada reserva. | Datos de las precondiciones | Según el plan, 409 INVALID_RESERVATION_STATE sin cambio de estado. | Pendiente | ReportNoShowServiceTest#invalidStatesAreRejectedWithoutClockOrWrites: verde. Prueba cuatro estados con mocks; no prueba todos los estados de la ficha. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReportNoShowServiceTest#invalidStatesAreRejectedWithoutClockOrWrites: verde. Prueba cuatro estados con mocks; no prueba todos los estados de la ficha. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La expiración pasiva corresponde a CU-08.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001, FR-006 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/ReportNoShowServiceTest.java#invalidStatesAreRejectedWithoutClockOrWrites | In Process
