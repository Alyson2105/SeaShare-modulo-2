### Test Case Specification: TC-CU07-002 — Estado no admisible
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estado no admisible. Requisitos: FR-001, FR-009. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El plan de CU-07, tabla de errores, incluye COMPLETED tanto en INVALID_RESERVATION_STATE como en TRIP_ALREADY_COMPLETED. Se comprueba RESERVED por HTTP y CANCELLED y COMPLETED en pruebas unitarias; faltan efectos persistidos para todos.

##### Dependencies & Assumptions
Evidencia V4 de componentes con dependencias controladas; los límites del escenario se detallan en Comments.

##### Preconditions & Default Setup
Preparar reservas que no estén IN_NAVIGATION.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationEndIntegrationTest#reservedReservationIsInvalidState (XML V4: passed). El XML confirma el método; sus aserciones comprueban 409 e INVALID_RESERVATION_STATE para RESERVED. El alcance restante incluye persistencia y otros estados. |
| 2 | Ejecutar | Solicitar cierre para cada una. | Datos de las precondiciones | Según el plan, 409 INVALID_RESERVATION_STATE; para COMPLETED prevé TRIP_ALREADY_COMPLETED; sin nueva transición. | Pendiente | NavigationEndIntegrationTest#reservedReservationIsInvalidState (XML V4: passed). El XML confirma el método; sus aserciones comprueban 409 e INVALID_RESERVATION_STATE para RESERVED. El alcance restante incluye persistencia y otros estados. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#reservedReservationIsInvalidState (XML V4: passed). El XML confirma el método; sus aserciones comprueban 409 e INVALID_RESERVATION_STATE para RESERVED. El alcance restante incluye persistencia y otros estados. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Estado COMPLETED es terminal; no equivale a idempotencia de respuesta.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: NavigationEndIntegrationTest#reservedReservationIsInvalidState. El plan de CU-07, tabla de errores, incluye COMPLETED tanto en INVALID_RESERVATION_STATE como en TRIP_ALREADY_COMPLETED. Se comprueba RESERVED por HTTP y CANCELLED y COMPLETED en pruebas unitarias; faltan efectos persistidos para todos.

#### Traceability
FR-001, FR-009 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#reservedReservationIsInvalidState | Draft

#### Decisión aprobada — 2026-11-13
Se aprobó TRIP_ALREADY_COMPLETED para un segundo cierre de una reserva COMPLETED. Esta decisión no modifica automáticamente el veredicto del TC.
