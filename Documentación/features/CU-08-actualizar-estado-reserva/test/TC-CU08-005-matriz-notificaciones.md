### Test Case Specification: TC-CU08-005 — Notificación según destino
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar notificación según destino. Requisitos: FR-007, FR-008, FR-011. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-08 es un puerto interno, sin endpoint HTTP. Outbox no prueba entrega; matriz excluye EXPIRED y PAYMENT_FAILED en M3 pese a FR-008 general.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas preparadas para transiciones válidas y outbox aislado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Transicionar a PENDING_PAYMENT, RESERVED e IN_NAVIGATION en orden. | Datos de las precondiciones | PENDING_PAYMENT encola AMQP sin M1; RESERVED e IN_NAVIGATION encolan AMQP y M1_HTTP con estado operativo correspondiente. | Pendiente | Integración: ChangeReservationStatusServiceTest#initiatedAndPendingPaymentDoNotEnqueueModuleOne, #enqueuesReservedToModuleOneOnPaymentConfirmed y #checkInAndCheckOutTransitionsWork pasaron con puertos simulados. Falta verificar cada destino operativo y fila persistida en una secuencia completa. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Outbox no prueba entrega; matriz excluye EXPIRED y PAYMENT_FAILED en M3 pese a FR-008 general.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Evidencia parcial; consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-007, FR-008, FR-011 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#initiatedAndPendingPaymentDoNotEnqueueModuleOne; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#enqueuesReservedToModuleOneOnPaymentConfirmed; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#checkInAndCheckOutTransitionsWork | In Process
