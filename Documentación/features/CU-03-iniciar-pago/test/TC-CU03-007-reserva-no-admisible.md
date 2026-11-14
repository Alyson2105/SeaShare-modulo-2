### Test Case Specification: TC-CU03-007 — Estado o TTL no admisible
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estado o ttl no admisible. Requisitos: FR-002, FR-007. Criterios relacionados: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
expiredTtlIsConflict comprueba el TTL separadamente; faltan HTTP 409 y ausencia de efectos persistidos.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java); no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Preparar por separado reserva con estado distinto de INITIATED y reserva INITIATED con TTL vencido; controlar reloj.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | StartPaymentServiceTest#nonInitiatedReservationIsConflict (XML V4: passed). Aserta INVALID_RESERVATION_STATE a nivel de servicio con mocks. expiredTtlIsConflict comprueba el TTL separadamente; faltan HTTP 409 y ausencia de efectos persistidos. |
| 2 | Ejecutar | Intentar pagar cada reserva. | Datos de las precondiciones | Según el plan, 409 INVALID_RESERVATION_STATE o 409 RESERVATION_EXPIRED respectivamente, sin iniciar pago. | Pendiente | StartPaymentServiceTest#nonInitiatedReservationIsConflict (XML V4: passed). Aserta INVALID_RESERVATION_STATE a nivel de servicio con mocks. expiredTtlIsConflict comprueba el TTL separadamente; faltan HTTP 409 y ausencia de efectos persistidos. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | StartPaymentServiceTest#nonInitiatedReservationIsConflict (XML V4: passed). Aserta INVALID_RESERVATION_STATE a nivel de servicio con mocks. expiredTtlIsConflict comprueba el TTL separadamente; faltan HTTP 409 y ausencia de efectos persistidos. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
El vencimiento exacto se verifica con reloj controlado; la expiración pasiva es CU-08.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: StartPaymentServiceTest#nonInitiatedReservationIsConflict. expiredTtlIsConflict comprueba el TTL separadamente; faltan HTTP 409 y ausencia de efectos persistidos.

#### Traceability
FR-002, FR-007 | SC-001 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#nonInitiatedReservationIsConflict; src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#expiredTtlIsConflict | In Process
