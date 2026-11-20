### Test Case Specification: TC-CU02-005 — Repetición idempotente
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU02-005
**Product Name & Version** : SeaShare M2 v0.1
**Author** : Juan Manuel
**Current Owner** : QA Lead
**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar que repetir una solicitud dentro de la ventana devuelve la reserva original. Trazabilidad: FR-011; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato | [plan](../plan.md) |
| Plan event | Eventos | [plan.event](../plan.event.md) |
| Context | Implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CreateReservationTest#sameIdempotencyKeyReturnsSameReservation comprueba una sola reserva por separado; no controla el límite temporal de 60 segundos en las peticiones ni todo SC-001.

##### Dependencies & Assumptions
La corrida utilizó backend y base de datos de pruebas; identidad temporal por headers sin Spring Security. [IdempotencyAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyAdapterTest.java); [IdempotencyClaimConcurrencyTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyClaimConcurrencyTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java).

##### Preconditions & Default Setup
Solicitud válida: `POST /api/v1/reservations`, `Content-Type: application/json`, `X-User-Id: <UUID>`, `X-User-Role: RENTER`, `Idempotency-Key: <UUID>`. Cuerpo: `{"vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","start_date":"2027-03-10","end_date":"2027-03-12","passengers":2,"departure_time":"08:30","estimated_total":"9600000.00","holder":{"full_name":"Carlos Gomez","phone":"+573001234567","email":"carlos@example.com"}}`. Usar identificadores únicos y flota disponible con capacidad suficiente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ejecutar escenario | Enviar dos veces el mismo cuerpo y clave UUID dentro de 60 segundos; consultar reservation y outbox. | Solicitud válida y variaciones descritas | Ambas respuestas 201 identifican la misma reserva; existe una sola reserva y un solo evento outbox correspondiente. | Pendiente | CreateReservationOutboxIntegrationTest#idempotentRepeatDoesNotEnqueueAdditionalMessage (XML V4: passed). Aserta dos HTTP 201, mismo reservation_id y una fila outbox. CreateReservationTest#sameIdempotencyKeyReturnsSameReservation comprueba una sola reserva por separado; no controla el límite temporal de 60 segundos en las peticiones ni todo SC-001. |

##### Section 2: Secondary Flow / Error Handling
No aplica como comportamiento principal adicional; consultar límites abajo.

##### Edge Cases & Boundary Conditions
El adaptador cubre reclamo de clave vencida; la prueba de concurrencia cubre solo el reclamo. plan.md menciona FR-019, inexistente en spec.md (FR-001 a FR-014).

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: CreateReservationOutboxIntegrationTest#idempotentRepeatDoesNotEnqueueAdditionalMessage. CreateReservationTest#sameIdempotencyKeyReturnsSameReservation comprueba una sola reserva por separado; no controla el límite temporal de 60 segundos en las peticiones ni todo SC-001.

#### Traceability
FR-011 | SC-001 | [IdempotencyAdapterTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyAdapterTest.java); [IdempotencyClaimConcurrencyTest](../../../../src/test/java/com/seashare/m2/reservas/integration/IdempotencyClaimConcurrencyTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java#sameIdempotencyKeyReturnsSameReservation; src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java#idempotentRepeatDoesNotEnqueueAdditionalMessage | In Process
