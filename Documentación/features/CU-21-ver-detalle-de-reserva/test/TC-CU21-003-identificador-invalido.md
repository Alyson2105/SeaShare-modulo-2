### Test Case Specification: TC-CU21-003 — Identificador inválido o inexistente
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU21-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar identificador inválido o inexistente. Requisitos: FR-001, FR-002. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No compara persistencia ni demuestra el SC-002 completo.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Identidad de prueba válida y UUID inexistente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#invalidUuidIs400 (XML V4: passed). Aserta 400 INVALID_UUID; ReservationQueryTest#unknownIdIs404 aserta 404 RESERVATION_NOT_FOUND. No compara persistencia ni demuestra el SC-002 completo. |
| 2 | Ejecutar | Consultar con identificador malformado y luego con UUID inexistente. | Datos de las precondiciones | 400 INVALID_UUID para formato malformado; 404 RESERVATION_NOT_FOUND para UUID inexistente. | Pendiente | ReservationQueryTest#invalidUuidIs400 (XML V4: passed). Aserta 400 INVALID_UUID; ReservationQueryTest#unknownIdIs404 aserta 404 RESERVATION_NOT_FOUND. No compara persistencia ni demuestra el SC-002 completo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#invalidUuidIs400 (XML V4: passed). Aserta 400 INVALID_UUID; ReservationQueryTest#unknownIdIs404 aserta 404 RESERVATION_NOT_FOUND. No compara persistencia ni demuestra el SC-002 completo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
No se asume UUID versión específica en el parser actual.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ReservationQueryTest#invalidUuidIs400. No compara persistencia ni demuestra el SC-002 completo.

#### Traceability
FR-001, FR-002 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java)  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#invalidUuidIs400 | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#unknownIdIs404 | In Process
