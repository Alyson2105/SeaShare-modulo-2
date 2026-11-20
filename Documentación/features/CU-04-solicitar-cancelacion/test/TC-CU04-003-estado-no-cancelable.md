### Test Case Specification: TC-CU04-003 — Rechazo por estado
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo por estado. Trazabilidad: FR-002, FR-004; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El servicio rechaza PENDING_PAYMENT con repositorio simulado; faltan los demás estados y su resultado HTTP y persistido.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Preparar reservas individuales en INITIATED, PENDING_PAYMENT, PAYMENT_FAILED, IN_NAVIGATION, COMPLETED, EXPIRED y CANCELLED.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | RequestCancellationServiceTest#reservationNotReservedIsRejected (XML V4: passed). Comprueba INVALID_RESERVATION_STATE y que CU-08 no se invoca para PENDING_PAYMENT; no prueba todos los estados. |
| 2 | Ejecutar | Solicitar cancelación activa de cada una. | Datos de las precondiciones | Según el plan, 409 INVALID_RESERVATION_STATE sin nueva transición. | Pendiente | RequestCancellationServiceTest#reservationNotReservedIsRejected (XML V4: passed). Comprueba INVALID_RESERVATION_STATE y que CU-08 no se invoca para PENDING_PAYMENT; no prueba todos los estados. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | RequestCancellationServiceTest#reservationNotReservedIsRejected (XML V4: passed). Comprueba INVALID_RESERVATION_STATE y que CU-08 no se invoca para PENDING_PAYMENT; no prueba todos los estados. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La expiración pasiva pertenece a CU-08.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: RequestCancellationServiceTest#reservationNotReservedIsRejected. El servicio rechaza PENDING_PAYMENT con repositorio simulado; faltan los demás estados y su resultado HTTP y persistido.

#### Traceability
FR-002, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#reservationNotReservedIsRejected | In Process
