### Test Case Specification: TC-CU14-002 — Payload para M3
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU14-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar payload para m3. Requisitos: FR-001, FR-002, FR-003, FR-004. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Spec FR-002 a FR-004 exige actor, subtipo, anticipación y notas; plan.md §4 y §6 define un payload distinto que no los incluye.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Transiciones publicables y datos de actor, subtipo y notas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | OutboxAdapterTest#cancelledPayloadUsesFinanceStatusAndOmitsSubtype (XML V4: passed). Aserta estados CANCELLED_* y ausencia del campo cancellation_subtype en el payload entregado a repositorio simulado. Spec FR-002 a FR-004 exige actor, subtipo, anticipación y notas; plan.md §4 y §6 define un payload distinto que no los incluye. |
| 2 | Ejecutar | Inspeccionar payload serializado y comparar con contrato de M3. | Datos de las precondiciones | El plan exige estados PENDING y CANCELLED_* y datos aplicables; token de pago obligatorio para PENDING. | Pendiente | OutboxAdapterTest#cancelledPayloadUsesFinanceStatusAndOmitsSubtype (XML V4: passed). Aserta estados CANCELLED_* y ausencia del campo cancellation_subtype en el payload entregado a repositorio simulado. Spec FR-002 a FR-004 exige actor, subtipo, anticipación y notas; plan.md §4 y §6 define un payload distinto que no los incluye. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | OutboxAdapterTest#cancelledPayloadUsesFinanceStatusAndOmitsSubtype (XML V4: passed). Aserta estados CANCELLED_* y ausencia del campo cancellation_subtype en el payload entregado a repositorio simulado. Spec FR-002 a FR-004 exige actor, subtipo, anticipación y notas; plan.md §4 y §6 define un payload distinto que no los incluye. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El adaptador traduce a PENDING y CANCELLED_*; el spec exige actor, subtipo separado, anticipación y notas que no figuran en el plan ni en el payload actual.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: OutboxAdapterTest#cancelledPayloadUsesFinanceStatusAndOmitsSubtype. Spec FR-002 a FR-004 exige actor, subtipo, anticipación y notas; plan.md §4 y §6 define un payload distinto que no los incluye.

#### Traceability
FR-001, FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java#statusChangedUsesFinanceStatusNames; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java#pendingIncludesPaymentTokenRef; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java#cancelledPayloadUsesFinanceStatusAndOmitsSubtype | Draft
