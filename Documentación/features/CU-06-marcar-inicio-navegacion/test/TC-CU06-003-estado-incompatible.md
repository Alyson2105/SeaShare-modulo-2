### Test Case Specification: TC-CU06-003 — Estado no admisible
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estado no admisible. Requisitos: FR-001, FR-004. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La prueba de servicio cubre tres estados con mocks; no acredita todos los estados enumerados en FR-004 con respuesta HTTP y persistencia.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-06.

##### Preconditions & Default Setup
Reservas en INITIATED, PENDING_PAYMENT, PAYMENT_FAILED, CANCELLED, COMPLETED y EXPIRED.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | StartNavigationServiceTest#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites (XML V4: passed). Aserta INVALID_RESERVATION_STATE para tres estados y verifica ausencia de escrituras sobre dependencias simuladas. |
| 2 | Ejecutar | Intentar iniciar navegación para cada reserva. | Datos de las precondiciones | Según el plan, 409 INVALID_RESERVATION_STATE y sin mutación. | Pendiente | StartNavigationServiceTest#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites (XML V4: passed). Aserta INVALID_RESERVATION_STATE para tres estados y verifica ausencia de escrituras sobre dependencias simuladas. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | StartNavigationServiceTest#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites (XML V4: passed). Aserta INVALID_RESERVATION_STATE para tres estados y verifica ausencia de escrituras sobre dependencias simuladas. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Para IN_NAVIGATION el plan prevé 409 TRIP_ALREADY_STARTED, sin duplicar efectos.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: StartNavigationServiceTest#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites. La prueba de servicio cubre tres estados con mocks; no acredita todos los estados enumerados en FR-004 con respuesta HTTP y persistencia.

#### Traceability
FR-001, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/StartNavigationServiceTest.java#pendingPaymentCancelledAndCompletedAreInvalidStateWithoutWrites | In Process
