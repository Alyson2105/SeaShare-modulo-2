### Test Case Specification: TC-CU02-001 — Creación exitosa
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU02-001
**Product Name & Version** : SeaShare M2 v0.1
**Author** : Juan Manuel
**Current Owner** : QA Lead
**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar la creación persistida de una reserva válida. Trazabilidad: FR-001, FR-004, FR-011, FR-012, FR-013; SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato | [plan](../plan.md) |
| Plan event | Eventos | [plan.event](../plan.event.md) |
| Context | Implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Flota fake por defecto; la integración de prueba usa puertos de flota simulados.

##### Dependencies & Assumptions
Backend y base de datos de pruebas disponibles para una ejecución futura; identidad temporal por headers sin Spring Security. [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); [CreateReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java).

##### Preconditions & Default Setup
Solicitud válida: `POST /api/v1/reservations`, `Content-Type: application/json`, `X-User-Id: <UUID>`, `X-User-Role: RENTER`, `Idempotency-Key: <UUID>`. Cuerpo: `{"vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","start_date":"2027-03-10","end_date":"2027-03-12","passengers":2,"departure_time":"08:30","estimated_total":"9600000.00","holder":{"full_name":"Carlos Gomez","phone":"+573001234567","email":"carlos@example.com"}}`. Usar identificadores únicos y flota disponible con capacidad suficiente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ejecutar escenario | Enviar la solicitud válida y consultar respuesta, reservation, auditoría y outbox por reservation_id. | Solicitud válida y variaciones descritas | 201 Created, Location /api/v1/reservations/{reservation_id}, estado INITIATED y ttl_minutes 15; reservation y ttl_expires_at persistidos; auditoría de creación y una fila reservation.info.provided para M3; sin M1_HTTP. estimated_total se conserva sin cálculo. | Pendiente | Integración: CreateReservationTest#createsReservationInInitiated pasó: 201, Location, estado, monto y TTL de respuesta; CreateReservationOutboxIntegrationTest#createsOutboxMessageWithExactSevenFieldsAndBoatId pasó: evento AMQP. Falta comprobar en este escenario auditoría, TTL persistido y ausencia de M1_HTTP. |

##### Section 2: Secondary Flow / Error Handling
No aplica como comportamiento principal adicional; consultar límites abajo.

##### Edge Cases & Boundary Conditions
El relay y la UI están fuera de alcance.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Evidencia parcial; los resultados pendientes se detallan en el flujo.

#### Traceability
FR-001, FR-004, FR-011, FR-012, FR-013 | SC-001, SC-003 | [CreateReservationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); [CreateReservationServiceTest](../../../../src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java); [CreateReservationOutboxIntegrationTest](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java#createsReservationInInitiated; src/test/java/com/seashare/m2/reservas/integration/CreateReservationOutboxIntegrationTest.java#createsOutboxMessageWithExactSevenFieldsAndBoatId; src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java#creationDelegatesToInitiateUseCase | In Process
