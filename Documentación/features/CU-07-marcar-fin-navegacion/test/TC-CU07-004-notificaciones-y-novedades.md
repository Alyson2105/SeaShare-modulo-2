### Test Case Specification: TC-CU07-004 — Notificación de cierre
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar notificación de cierre. Requisitos: FR-003, FR-006, FR-007, FR-008. Criterios: SC-002, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La prueba unitaria captura novedades hacia CU-08 simulado; no acredita entrega real a M1/M3, payload íntegro ni el SLA de SC-002.

##### Dependencies & Assumptions
Evidencia V4 de componentes con dependencias controladas; los límites del escenario se detallan en Comments.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION con novedades y guarda m1_blocked; observar outbox.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage (XML V4: passed). El captor comprueba completionNotes y verify comprueba checkout con novedades; CU-08 es un mock y no prueba entrega externa. |
| 2 | Ejecutar | Confirmar cierre y consultar mensajes asociados. | Datos de las precondiciones | Solicitar MAINTENANCE_CLEANING a M1 y comunicar COMPLETED y novedades a M3; sin cálculo monetario en M2. | Pendiente | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage (XML V4: passed). El captor comprueba completionNotes y verify comprueba checkout con novedades; CU-08 es un mock y no prueba entrega externa. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage (XML V4: passed). El captor comprueba completionNotes y verify comprueba checkout con novedades; CU-08 es un mock y no prueba entrega externa. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Encolar no demuestra entrega ni plazo de primer intento; payload final M3 pendiente T-12.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage. La prueba unitaria captura novedades hacia CU-08 simulado; no acredita entrega real a M1/M3, payload íntegro ni el SLA de SC-002.

#### Traceability
FR-003, FR-006, FR-007, FR-008 | SC-002, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#remarksArePassedAsCompletionNotesAndChangeMessage | In Process
