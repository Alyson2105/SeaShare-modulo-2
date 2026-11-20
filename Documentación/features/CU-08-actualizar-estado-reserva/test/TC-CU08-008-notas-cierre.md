### Test Case Specification: TC-CU08-008 — Novedades de cierre
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-008

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar novedades de cierre. Requisitos: FR-009, FR-010. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
checkInAndCheckOutTransitionsWork captura las notas del DTO por separado; no acredita el primer intento externo en 500 ms de SC-002 ni ausencia global de cálculo.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION; comando COMPLETED con completion_notes.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ChangeReservationStatusServiceTest#persistsCompletionNotesOnCompleted (XML V4: passed). Aserta notas de cierre persistidas en reserva. checkInAndCheckOutTransitionsWork captura las notas del DTO por separado; no acredita el primer intento externo en 500 ms de SC-002 ni ausencia global de cálculo. |
| 2 | Ejecutar | Ejecutar transición y consultar reserva y evento encolado. | Datos de las precondiciones | Se conservan novedades en el dominio y se incluyen en el DTO del evento; M2 no calcula dinero. | Pendiente | ChangeReservationStatusServiceTest#persistsCompletionNotesOnCompleted (XML V4: passed). Aserta notas de cierre persistidas en reserva. checkInAndCheckOutTransitionsWork captura las notas del DTO por separado; no acredita el primer intento externo en 500 ms de SC-002 ni ausencia global de cálculo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ChangeReservationStatusServiceTest#persistsCompletionNotesOnCompleted (XML V4: passed). Aserta notas de cierre persistidas en reserva. checkInAndCheckOutTransitionsWork captura las notas del DTO por separado; no acredita el primer intento externo en 500 ms de SC-002 ni ausencia global de cálculo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Payload definitivo de M3 pendiente T-12; el SLA de primer intento no se prueba aquí.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ChangeReservationStatusServiceTest#persistsCompletionNotesOnCompleted. checkInAndCheckOutTransitionsWork captura las notas del DTO por separado; no acredita el primer intento externo en 500 ms de SC-002 ni ausencia global de cálculo.

#### Traceability
FR-009, FR-010 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#persistsCompletionNotesOnCompleted; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#checkInAndCheckOutTransitionsWork | In Process
