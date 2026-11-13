### Test Case Specification: TC-CU02-004 — Cálculo y persistencia TTL
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU02-004
**Product Name & Version** : SeaShare M2 v0.1
**Author** : Juan Manuel
**Current Owner** : QA Lead
**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar el cálculo y almacenamiento del TTL de creación. Trazabilidad: FR-012; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato | [plan](../plan.md) |
| Plan event | Eventos | [plan.event](../plan.event.md) |
| Context | Implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La expiración pasiva pertenece al motor de estados y CU-08; no probar aquí el job.

##### Dependencies & Assumptions
Backend y base de datos de pruebas disponibles para una ejecución futura; identidad temporal por headers sin Spring Security. [TtlPolicyTest](../../../../src/test/java/com/seashare/m2/reservas/domain/service/TtlPolicyTest.java); [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java).

##### Preconditions & Default Setup
Solicitud válida: `POST /api/v1/reservations`, `Content-Type: application/json`, `X-User-Id: <UUID>`, `X-User-Role: RENTER`, `Idempotency-Key: <UUID>`. Cuerpo: `{"vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","start_date":"2027-03-10","end_date":"2027-03-12","passengers":2,"departure_time":"08:30","estimated_total":"9600000.00","holder":{"full_name":"Carlos Gomez","phone":"+573001234567","email":"carlos@example.com"}}`. Usar identificadores únicos y flota disponible con capacidad suficiente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ejecutar escenario | Crear con ClockPort controlado; comparar created_at y ttl_expires_at de respuesta y fila persistida. | Solicitud válida y variaciones descritas | ttl_expires_at = created_at + exactamente 900 segundos, con precisión del instante almacenado; ambas marcas corresponden al mismo reloj. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica como comportamiento principal adicional; consultar límites abajo.

##### Edge Cases & Boundary Conditions
En el límite de 900 segundos la política considera vencida la reserva; la transición no se verifica aquí.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Registrar resultado y conservar identificadores para consultar las filas; la documentación no implica ejecución.

#### Traceability
FR-012 | SC-001 | [TtlPolicyTest](../../../../src/test/java/com/seashare/m2/reservas/domain/service/TtlPolicyTest.java); [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | In Process
