### Test Case Specification: TC-CU22-004 — Payload operativo e identidad
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU22-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar payload operativo e identidad. Requisitos: FR-003, FR-004, FR-005, FR-007. Criterios: SC-003, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-22 encola destino M1_HTTP; el adaptador actual es fake y no realiza HTTP real. No confundir identidad de evento con deduplicación demostrada por M1.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [outbox](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Transición notificable con evento M1_HTTP.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar outbox y auditoría. | Datos de las precondiciones | Payload contiene vessel_id, operational_status, occurred_at y event_id; ID coincide con auditoría y no hay importes. | Pendiente | No ejecutado |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
No confundir identidad de evento con deduplicación demostrada por M1.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Registrar el resultado cuando el flujo esté disponible y conservar los identificadores de prueba.

#### Traceability
FR-003, FR-004, FR-005, FR-007 | SC-003, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [outbox](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/messaging/outbox/OutboxAdapterTest.java) | In Process
