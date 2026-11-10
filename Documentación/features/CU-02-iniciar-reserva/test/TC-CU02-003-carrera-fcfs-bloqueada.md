### Test Case Specification: TC-CU02-003 — Carrera FCFS
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU02-003
**Product Name & Version** : SeaShare M2 v0.1
**Author** : Juan Manuel
**Current Owner** : QA Lead
**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar en el futuro la exclusividad FCFS al iniciar pago. Trazabilidad: FR-014; SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato | [plan](../plan.md) |
| Plan event | Eventos | [plan.event](../plan.event.md) |
| Context | Implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Bloqueado: transición a PENDING_PAYMENT pertenece a CU-03, aún no implementado; no existe endpoint de pago ni bloqueo pesimista correspondiente.

##### Dependencies & Assumptions
Backend y base de datos de pruebas disponibles para una ejecución futura; identidad temporal por headers sin Spring Security. Sin test ejecutable de CU-03.

##### Preconditions & Default Setup
Solicitud válida: `POST /api/v1/reservations`, `Content-Type: application/json`, `X-User-Id: <UUID>`, `X-User-Role: RENTER`, `Idempotency-Key: <UUID>`. Cuerpo: `{"vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","start_date":"2027-03-10","end_date":"2027-03-12","passengers":2,"departure_time":"08:30","estimated_total":"9600000.00","holder":{"full_name":"Carlos Gomez","phone":"+573001234567","email":"carlos@example.com"}}`. Usar identificadores únicos y flota disponible con capacidad suficiente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ejecutar escenario | Tras implementar CU-03, iniciar simultáneamente pago para dos reservas INITIATED con mismas fechas y embarcación. | Solicitud válida y variaciones descritas | Una transacción gana; competidor perdedor recibe 409 Conflict según spec. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica como comportamiento principal adicional; consultar límites abajo.

##### Edge Cases & Boundary Conditions
IdempotencyClaimConcurrencyTest solo prueba concurrencia de reclamo de clave y no evidencia FCFS. Sin test ejecutable de CU-03. La coexistencia inicial no prueba la carrera.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Registrar resultado y conservar identificadores para consultar las filas; la documentación no implica ejecución.

#### Traceability
FR-014 | SC-002 | Sin test ejecutable de CU-03 | Blocked
