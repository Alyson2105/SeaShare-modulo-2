### Test Case Specification: TC-CU08-007 — Auditoría de transición
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar auditoría de transición. Requisitos: FR-004, FR-011. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No compara todas las propiedades de auditoría con filas persistidas de auditoría y outbox.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [auditoria](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationStatusAuditImmutabilityTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva válida y transición que encola eventos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ChangeReservationStatusServiceTest#transitionAuditCarriesActorIdAndEventIds (XML V4: passed). Verifica mediante espía sobre el puerto de auditoría actorId y un identificador de evento, además de destino AMQP en resultado. No compara todas las propiedades de auditoría con filas persistidas de auditoría y outbox. |
| 2 | Ejecutar | Ejecutar transición e inspeccionar auditoría y outbox. | Datos de las precondiciones | Auditoría incluye actor, estado anterior y nuevo, instante e IDs que corresponden a eventos encolados en la transacción. | Pendiente | ChangeReservationStatusServiceTest#transitionAuditCarriesActorIdAndEventIds (XML V4: passed). Verifica mediante espía sobre el puerto de auditoría actorId y un identificador de evento, además de destino AMQP en resultado. No compara todas las propiedades de auditoría con filas persistidas de auditoría y outbox. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ChangeReservationStatusServiceTest#transitionAuditCarriesActorIdAndEventIds (XML V4: passed). Verifica mediante espía sobre el puerto de auditoría actorId y un identificador de evento, además de destino AMQP en resultado. No compara todas las propiedades de auditoría con filas persistidas de auditoría y outbox. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La inmutabilidad de auditoría se prueba por separado; relay externo fuera de alcance.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ChangeReservationStatusServiceTest#transitionAuditCarriesActorIdAndEventIds. No compara todas las propiedades de auditoría con filas persistidas de auditoría y outbox.

#### Traceability
FR-004, FR-011 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [auditoria](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationStatusAuditImmutabilityTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#transitionAuditCarriesActorIdAndEventIds | In Process
