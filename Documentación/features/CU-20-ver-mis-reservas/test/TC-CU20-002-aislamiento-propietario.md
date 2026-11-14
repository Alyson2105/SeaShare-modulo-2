### Test Case Specification: TC-CU20-002 — Listado de propietario
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU20-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar listado de propietario. Requisitos: FR-001, FR-002. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Las tres reservas comparten propietario; no prueba exclusión frente a otro propietario ni JWT.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Dos propietarios con reservas distintas; X-User-Id UUID y X-User-Role OWNER.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#listsOnlyOwnReservations (XML V4: passed). Aserta HTTP 200 y tres resultados del propietario. Las tres reservas comparten propietario; no prueba exclusión frente a otro propietario ni JWT. |
| 2 | Ejecutar | Consultar listado como un propietario. | Datos de las precondiciones | 200 con solo las reservas cuyo owner_id corresponde a la identidad de consulta. | Pendiente | ReservationQueryTest#listsOnlyOwnReservations (XML V4: passed). Aserta HTTP 200 y tres resultados del propietario. Las tres reservas comparten propietario; no prueba exclusión frente a otro propietario ni JWT. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#listsOnlyOwnReservations (XML V4: passed). Aserta HTTP 200 y tres resultados del propietario. Las tres reservas comparten propietario; no prueba exclusión frente a otro propietario ni JWT. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El filtro local por owner_id no prueba la autenticidad de headers ni pertenencia real en M1.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ReservationQueryTest#listsOnlyOwnReservations. Las tres reservas comparten propietario; no prueba exclusión frente a otro propietario ni JWT.

#### Traceability
FR-001, FR-002 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java)  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsOnlyOwnReservations | In Process
