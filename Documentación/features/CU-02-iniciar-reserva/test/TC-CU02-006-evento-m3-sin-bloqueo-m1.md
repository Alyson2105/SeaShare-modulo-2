### Test Case Specification: TC-CU02-006 — Evento M3 y ausencia de bloqueo M1
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU02-006
**Product Name & Version** : SeaShare M2 v0.1
**Author** : Juan Manuel
**Current Owner** : QA Lead
**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar el destino de información de creación y la ausencia de notificación de inventario. Trazabilidad: FR-013; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato | [plan](../plan.md) |
| Plan event | Eventos | [plan.event](../plan.event.md) |
| Context | Implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El relay efectivo requiere servicios y queda fuera; la matriz verifica ausencia de notificación M1 para INITIATED.

##### Dependencies & Assumptions
Backend y base de datos de pruebas disponibles para una ejecución futura; identidad temporal por headers sin Spring Security. [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java); [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java).

##### Preconditions & Default Setup
Solicitud válida: `POST /api/v1/reservations`, `Content-Type: application/json`, `X-User-Id: <UUID>`, `X-User-Role: RENTER`, `Idempotency-Key: <UUID>`. Cuerpo: `{"vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","start_date":"2027-03-10","end_date":"2027-03-12","passengers":2,"departure_time":"08:30","estimated_total":"9600000.00","holder":{"full_name":"Carlos Gomez","phone":"+573001234567","email":"carlos@example.com"}}`. Usar identificadores únicos y flota disponible con capacidad suficiente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ejecutar escenario | Crear reserva y consultar outbox por reservation_id; inspeccionar la decisión de la matriz para INITIATED. | Solicitud válida y variaciones descritas | Una fila AMQP reservation.info.provided para M3; ninguna fila M1_HTTP ni envío o bloqueo de inventario M1 en INITIATED. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica como comportamiento principal adicional; consultar límites abajo.

##### Edge Cases & Boundary Conditions
Evento AMQP para M3 y notificación HTTP a M1 son mecanismos distintos.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Registrar resultado y conservar identificadores para consultar las filas; la documentación no implica ejecución.

#### Traceability
FR-013 | SC-001 | [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java); [ModuleOneNotificationMatrixTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | In Process
