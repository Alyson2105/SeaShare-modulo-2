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
Verifica primer 200 y segundo 409; spec exige respuesta correcta al reintento y plan precisa 409, queda decisión contractual y efectos completos sin verificar. El endpoint y servicio existen. Edge case dice responder correctamente al reintento, pero el plan prevé 409 TRIP_ALREADY_COMPLETED; no define respuesta idempotente.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-07.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION; solicitudes repetidas de cierre bajo mala conexión.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted: verde. Verifica primer 200 y segundo 409; spec exige respuesta correcta al reintento y plan precisa 409, queda decisión contractual y efectos completos sin verificar. |
| 2 | Ejecutar | Enviar cierre y repetir solicitud idéntica. | Datos de las precondiciones | Solo un cierre y un conjunto de efectos; la respuesta repetida requiere resolver el contrato. | Pendiente | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted: verde. Verifica primer 200 y segundo 409; spec exige respuesta correcta al reintento y plan precisa 409, queda decisión contractual y efectos completos sin verificar. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#secondClickReturnsTripAlreadyCompleted: verde. Verifica primer 200 y segundo 409; spec exige respuesta correcta al reintento y plan precisa 409, queda decisión contractual y efectos completos sin verificar. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Edge case dice responder correctamente al reintento, pero el plan prevé 409 TRIP_ALREADY_COMPLETED; no define respuesta idempotente.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Draft; evidencia parcial.

#### Traceability
FR-001, FR-004, FR-005 | SC-001, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#secondClickReturnsTripAlreadyCompleted | Draft
