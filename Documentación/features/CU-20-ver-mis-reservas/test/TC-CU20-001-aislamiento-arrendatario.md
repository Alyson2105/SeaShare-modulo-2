### Test Case Specification: TC-CU20-001 — Listado de arrendatario
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU20-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar listado de arrendatario. Requisitos: FR-001, FR-002, FR-003. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No afirma los identificadores individuales ni todos los campos exigidos; identidad por headers, no JWT.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Dos arrendatarios con reservas distintas; cabeceras X-User-Id UUID y X-User-Role RENTER.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#listsOnlyOwnReservations (XML V4: passed). Aserta HTTP 200, total 2 y longitud 2 con dos reservas propias y una ajena; comprueba campos de respuesta. No afirma los identificadores individuales ni todos los campos exigidos; identidad por headers, no JWT. |
| 2 | Ejecutar | Consultar GET /api/v1/reservations como uno de ellos. | Datos de las precondiciones | 200 con items pertenecientes al arrendatario, fechas, estado, vessel_id y estimated_total; excluye reservas del otro. | Pendiente | ReservationQueryTest#listsOnlyOwnReservations (XML V4: passed). Aserta HTTP 200, total 2 y longitud 2 con dos reservas propias y una ajena; comprueba campos de respuesta. No afirma los identificadores individuales ni todos los campos exigidos; identidad por headers, no JWT. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#listsOnlyOwnReservations (XML V4: passed). Aserta HTTP 200, total 2 y longitud 2 con dos reservas propias y una ajena; comprueba campos de respuesta. No afirma los identificadores individuales ni todos los campos exigidos; identidad por headers, no JWT. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Identidad por headers es temporal; JWT no está implementado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ReservationQueryTest#listsOnlyOwnReservations. No afirma los identificadores individuales ni todos los campos exigidos; identidad por headers, no JWT.

#### Traceability
FR-001, FR-002, FR-003 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java)  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsOnlyOwnReservations | In Process
