### Test Case Specification: TC-CU21-001 — Detalle de reserva propia
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU21-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar detalle de reserva propia. Requisitos: FR-001, FR-002, FR-003. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No comprueba todos los campos del escenario ni servicios reales; identidad por headers.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva existente y cabeceras X-User-Id UUID y X-User-Role RENTER u OWNER perteneciente a ella.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#detailIncludesContractFields (XML V4: passed). Aserta 200, ID, estado RESERVED, itinerario y datos de embarcación fake. No comprueba todos los campos del escenario ni servicios reales; identidad por headers. |
| 2 | Ejecutar | Consultar GET /api/v1/reservations/{id}. | Datos de las precondiciones | 200 con reservation_id, status, vessel_id, fechas, pasajeros, holder y estimated_total. | Pendiente | ReservationQueryTest#detailIncludesContractFields (XML V4: passed). Aserta 200, ID, estado RESERVED, itinerario y datos de embarcación fake. No comprueba todos los campos del escenario ni servicios reales; identidad por headers. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#detailIncludesContractFields (XML V4: passed). Aserta 200, ID, estado RESERVED, itinerario y datos de embarcación fake. No comprueba todos los campos del escenario ni servicios reales; identidad por headers. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La respuesta expone subtipo y campos enriquecidos; no demuestra origen oficial de los importes.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ReservationQueryTest#detailIncludesContractFields. No comprueba todos los campos del escenario ni servicios reales; identidad por headers.

#### Traceability
FR-001, FR-002, FR-003 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java)  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#detailIncludesContractFields | In Process
