### Test Case Specification: TC-CU07-006 — Duplicación de cierre
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar duplicación de cierre. Requisitos: FR-001, FR-004, FR-005. Criterios: SC-001, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-07: el segundo cierre devuelve TRIP_ALREADY_COMPLETED y no duplica checkout, disputa ni filas outbox en la prueba local. El código contractual de error sigue ambiguo.

##### Dependencies & Assumptions
Evidencia V4 de componentes con dependencias controladas; los límites del escenario se detallan en Comments.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION; solicitudes repetidas de cierre bajo mala conexión.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted (XML V4: passed). El método ejecutado comprueba primer HTTP 200 y segundo 409 TRIP_ALREADY_COMPLETED; no cuenta efectos persistidos ni resuelve la contradicción del plan. |
| 2 | Ejecutar | Enviar cierre y repetir solicitud idéntica. | Datos de las precondiciones | Solo un cierre y un conjunto de efectos; la respuesta repetida requiere resolver el contrato. | Pendiente | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted (XML V4: passed). El método ejecutado comprueba primer HTTP 200 y segundo 409 TRIP_ALREADY_COMPLETED; no cuenta efectos persistidos ni resuelve la contradicción del plan. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted (XML V4: passed). El método ejecutado comprueba primer HTTP 200 y segundo 409 TRIP_ALREADY_COMPLETED; no cuenta efectos persistidos ni resuelve la contradicción del plan. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El spec exige no duplicar registros y el plan presenta dos códigos para COMPLETED en su tabla de errores.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted. El plan de CU-07, tabla de errores, incluye COMPLETED tanto en INVALID_RESERVATION_STATE como en TRIP_ALREADY_COMPLETED; el edge case del spec solo exige responder correctamente. La ambigüedad afecta la respuesta esperada.

#### Traceability
FR-001, FR-004, FR-005 | SC-001, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#secondClickReturnsTripAlreadyCompleted | Draft

#### Evidencia local posterior a V4 — 2026-11-13
NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted; PostgreSQL local; corrida focalizada 2026-11-13, 18 pruebas sin fallos. Veredicto técnico: Partial. No acredita integración real M1/M3 ni resuelve las decisiones contractuales pendientes.
