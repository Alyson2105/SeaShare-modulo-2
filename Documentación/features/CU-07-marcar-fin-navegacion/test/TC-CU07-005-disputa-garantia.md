### Test Case Specification: TC-CU07-005 — Apertura de disputa
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar apertura de disputa. Requisitos: FR-004, FR-010. Criterios: SC-002, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
H10 conecta CU-07 con CU-16 en una transacción. La apertura directa y el rollback se prueban por separado; falta verificar en el cierre integrado el ID y vencimiento devueltos por HTTP, la disputa persistida asociada, los eventos y el SLA de SC-002.

##### Dependencies & Assumptions
Evidencia V4 de componentes con dependencias controladas; los límites del escenario se detallan en Comments.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION válida con CU-16 disponible y reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent prepara reserva IN_NAVIGATION y reloj controlado; EndNavigationServiceTest#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner captura el comando hacia CU-16 simulado. OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow comprueba PENDING y ventana de 24 h en apertura directa. NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation comprueba HTTP 500, reserva IN_NAVIGATION y cero filas checkout al fallar CU-16. Todos: XML V4 passed; falta comprobar ID, plazo y eventos en el cierre integrado. |
| 2 | Ejecutar | Confirmar cierre y consultar disputa asociada. | Datos de las precondiciones | Plan prevé disputa PENDING y ventana de 24 horas desde el cierre, con dispute_id y vencimiento. | Pendiente | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent prepara reserva IN_NAVIGATION y reloj controlado; EndNavigationServiceTest#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner captura el comando hacia CU-16 simulado. OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow comprueba PENDING y ventana de 24 h en apertura directa. NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation comprueba HTTP 500, reserva IN_NAVIGATION y cero filas checkout al fallar CU-16. Todos: XML V4 passed; falta comprobar ID, plazo y eventos en el cierre integrado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent prepara reserva IN_NAVIGATION y reloj controlado; EndNavigationServiceTest#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner captura el comando hacia CU-16 simulado. OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow comprueba PENDING y ventana de 24 h en apertura directa. NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation comprueba HTTP 500, reserva IN_NAVIGATION y cero filas checkout al fallar CU-16. Todos: XML V4 passed; falta comprobar ID, plazo y eventos en el cierre integrado. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La retención financiera corresponde a M3; no se demuestra su integración real.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NavigationEndDisputeRollbackTest#disputeFailureLeavesReservationInNavigation. H10 conecta CU-07 con CU-16 en una transacción. La apertura directa y el rollback se prueban por separado; falta verificar en el cierre integrado el ID y vencimiento devueltos por HTTP, la disputa persistida asociada, los eventos y el SLA de SC-002.

#### Traceability
FR-004, FR-010 | SC-002, SC-004 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/OpenDisputeIntegrationTest.java#opensPendingDisputeWith24HourWindow | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndDisputeRollbackTest.java#disputeFailureLeavesReservationInNavigation; src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#successfulEndPersistsArrivalCompletedStateAndCheckoutEvent; src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#completedCheckoutOpensDisputeWithStatusChangedAtAndOwner | In Process
