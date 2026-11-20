### Test Case Specification: TC-CU08-001 — Transición válida
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar transición válida. Requisitos: FR-002, FR-003, FR-004. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El método no aserta TTL persistido; la ausencia de M1 se prueba separadamente.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva INITIATED con TTL vigente y comando interno válido.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ChangeReservationStatusServiceTest#validChangeSavesStatusAndAudit (XML V4: passed). Aserta PENDING_PAYMENT persistido y versión incrementada; verifica llamadas a auditoría y outbox mediante espías. El método no aserta TTL persistido; la ausencia de M1 se prueba separadamente. |
| 2 | Ejecutar | Invocar ChangeReservationStatusUseCase hacia PENDING_PAYMENT. | Datos de las precondiciones | El estado se persiste como PENDING_PAYMENT y conserva ttl_expires_at; no hay notificación M1. | Pendiente | ChangeReservationStatusServiceTest#validChangeSavesStatusAndAudit (XML V4: passed). Aserta PENDING_PAYMENT persistido y versión incrementada; verifica llamadas a auditoría y outbox mediante espías. El método no aserta TTL persistido; la ausencia de M1 se prueba separadamente. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ChangeReservationStatusServiceTest#validChangeSavesStatusAndAudit (XML V4: passed). Aserta PENDING_PAYMENT persistido y versión incrementada; verifica llamadas a auditoría y outbox mediante espías. El método no aserta TTL persistido; la ausencia de M1 se prueba separadamente. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La creación de CU-02 delega en CU-08; la prueba directa del motor con reloj controlado queda pendiente.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ChangeReservationStatusServiceTest#validChangeSavesStatusAndAudit. El método no aserta TTL persistido; la ausencia de M1 se prueba separadamente.

#### Traceability
FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#validChangeSavesStatusAndAudit; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#initiatedAndPendingPaymentDoNotEnqueueModuleOne | In Process
