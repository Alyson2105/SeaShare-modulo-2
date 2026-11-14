### Test Case Specification: TC-CU07-001 — Cierre de navegación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar cierre de navegación. Requisitos: FR-001, FR-003, FR-004, FR-005. Criterios: SC-002, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La integración HTTP verifica el cierre con novedades y el registro persistido; no demuestra el caso sin novedades en una segunda reserva ni la notificación externa y su plazo de SC-002.

##### Dependencies & Assumptions
Evidencia V4 de componentes con dependencias controladas; los límites del escenario se detallan en Comments.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION y solicitante propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent (XML V4: passed). HTTP 200 y COMPLETED; compara hora real, propietario, novedades y una fila checkout en PostgreSQL. Falta ejecución integrada sin novedades y el primer intento a M1 en menos de un segundo. |
| 2 | Ejecutar | Registrar fin con y sin novedades en reservas separadas. | Datos de las precondiciones | Según el plan, 200 y COMPLETED; hora real y novedades se registran mediante CU-08 y check-out. | Pendiente | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent (XML V4: passed). HTTP 200 y COMPLETED; compara hora real, propietario, novedades y una fila checkout en PostgreSQL. Falta ejecución integrada sin novedades y el primer intento a M1 en menos de un segundo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent (XML V4: passed). HTTP 200 y COMPLETED; compara hora real, propietario, novedades y una fila checkout en PostgreSQL. Falta ejecución integrada sin novedades y el primer intento a M1 en menos de un segundo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Regreso temprano o tardío conserva la hora real sin cálculo monetario.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent. La integración HTTP verifica el cierre con novedades y el registro persistido; no demuestra el caso sin novedades en una segunda reserva ni la notificación externa y su plazo de SC-002.

#### Traceability
FR-001, FR-003, FR-004, FR-005 | SC-002, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent | In Process
