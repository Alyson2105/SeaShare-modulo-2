### Test Case Specification: TC-CU02-002 — Rechazo de datos inválidos
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU02-002
**Product Name & Version** : SeaShare M2 v0.1
**Author** : Juan Manuel
**Current Owner** : QA Lead
**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar el rechazo de entradas inválidas implementadas. Trazabilidad: FR-006, FR-007, FR-008; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato | [plan](../plan.md) |
| Plan event | Eventos | [plan.event](../plan.event.md) |
| Context | Implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La validación visual es UI; el formato telefónico no está implementado: solo no vacío y máximo 30 caracteres.

##### Dependencies & Assumptions
Backend y base de datos de pruebas disponibles para una ejecución futura; identidad temporal por headers sin Spring Security. [CreateReservationValidationTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java).

##### Preconditions & Default Setup
Solicitud válida: `POST /api/v1/reservations`, `Content-Type: application/json`, `X-User-Id: <UUID>`, `X-User-Role: RENTER`, `Idempotency-Key: <UUID>`. Cuerpo: `{"vessel_id":"d3b07384-d113-49cd-a5d6-812e9bcfc101","start_date":"2027-03-10","end_date":"2027-03-12","passengers":2,"departure_time":"08:30","estimated_total":"9600000.00","holder":{"full_name":"Carlos Gomez","phone":"+573001234567","email":"carlos@example.com"}}`. Usar identificadores únicos y flota disponible con capacidad suficiente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ejecutar escenario | Repetir con una única alteración por solicitud y claves nuevas: full_name ausente o vacío; phone ausente o vacío; email inválido; fechas inválidas o fin anterior a inicio; passengers cero o negativo; vessel_id ausente o UUID malformado; clave ausente o malformada; omitir cada header de identidad. | Solicitud válida y variaciones descritas | 400 para datos inválidos o clave ausente/malformada; 401 si falta identidad. Body con code y message; no se agrega reservation ni outbox. Clave ausente: INVALID_PARAMETERS; clave malformada: INVALID_UUID. | Pendiente | Integración: CreateReservationValidationTest#missingIdempotencyKeyIs400, #invalidIdempotencyKeyIs400InvalidUuid y #missingUserIdIs401 pasaron; CreateReservationTest#rejectsEndBeforeStart pasó. Faltan las demás variantes, message y ausencia de reservation y outbox para cada rechazo. |

##### Section 2: Secondary Flow / Error Handling
No aplica como comportamiento principal adicional; consultar límites abajo.

##### Edge Cases & Boundary Conditions
No exigir rechazo por un teléfono no vacío con formato inválido; es una brecha.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Evidencia parcial; los resultados pendientes se detallan en el flujo.

#### Traceability
FR-006, FR-007, FR-008 | SC-001 | [CreateReservationValidationTest](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java#missingIdempotencyKeyIs400; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java#invalidIdempotencyKeyIs400InvalidUuid; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java#missingUserIdIs401; src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java#rejectsEndBeforeStart | In Process
