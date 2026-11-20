### Test Case Specification: TC-CU03-008 — Evento M3 sin bloqueo M1
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-008

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar evento m3 sin bloqueo m1. Requisitos: FR-006, FR-009. Criterios relacionados: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Falta inspeccionar payload serializado y ausencia de notificación M1 en flujo integrado.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva válida y dependencias simuladas; observar outbox y adaptador de notificación.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | PaymentIntegrationTest#pendingEventCarriesPaymentToken (XML V4: passed). Verifica enqueue en outbox simulado de evento interno con PENDING_PAYMENT y token. Falta inspeccionar payload serializado y ausencia de notificación M1 en flujo integrado. |
| 2 | Ejecutar | Iniciar pago y examinar el evento de estado y las notificaciones M1. | Datos de las precondiciones | El plan exige evento reservation.status.changed con estado PENDING y token para M3; no hay notificación M1_HTTP al pasar a PENDING_PAYMENT. | Pendiente | PaymentIntegrationTest#pendingEventCarriesPaymentToken (XML V4: passed). Verifica enqueue en outbox simulado de evento interno con PENDING_PAYMENT y token. Falta inspeccionar payload serializado y ausencia de notificación M1 en flujo integrado. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | PaymentIntegrationTest#pendingEventCarriesPaymentToken (XML V4: passed). Verifica enqueue en outbox simulado de evento interno con PENDING_PAYMENT y token. Falta inspeccionar payload serializado y ausencia de notificación M1 en flujo integrado. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
CU-08 emite estado interno PENDING_PAYMENT; el adaptador lo traduce a PENDING para M3. Relay y cobro real fuera de alcance.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: PaymentIntegrationTest#pendingEventCarriesPaymentToken. Falta inspeccionar payload serializado y ausencia de notificación M1 en flujo integrado.

#### Traceability
FR-006, FR-009 | SC-001 | [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#pendingEventCarriesPaymentToken; src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#initiatedAndPendingPaymentNotifyNobody | In Process
