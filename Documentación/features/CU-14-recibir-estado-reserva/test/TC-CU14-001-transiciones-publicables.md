### Test Case Specification: TC-CU14-001 — Encolado de transiciones
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU14-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar encolado de transiciones. Requisitos: FR-001, FR-006. Criterios: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No verifica todas las transiciones publicables ni sus filas outbox persistidas y entrega.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas con transiciones válidas a PENDING_PAYMENT, RESERVED, IN_NAVIGATION, COMPLETED y CANCELLED.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ModuleOneNotificationMatrixTest#expiredAndPaymentFailedNotifyNobody (XML V4: passed). Aserta que EXPIRED y PAYMENT_FAILED no notifican a M3 ni M1 en la matriz. No verifica todas las transiciones publicables ni sus filas outbox persistidas y entrega. |
| 2 | Ejecutar | Ejecutar cada transición e inspeccionar outbox AMQP. | Datos de las precondiciones | El motor encola evento de estado para cada destino publicable; INITIATED, EXPIRED y PAYMENT_FAILED no generan evento de cambio de estado. | Pendiente | ModuleOneNotificationMatrixTest#expiredAndPaymentFailedNotifyNobody (XML V4: passed). Aserta que EXPIRED y PAYMENT_FAILED no notifican a M3 ni M1 en la matriz. No verifica todas las transiciones publicables ni sus filas outbox persistidas y entrega. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ModuleOneNotificationMatrixTest#expiredAndPaymentFailedNotifyNobody (XML V4: passed). Aserta que EXPIRED y PAYMENT_FAILED no notifican a M3 ni M1 en la matriz. No verifica todas las transiciones publicables ni sus filas outbox persistidas y entrega. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-02 sí encola reservation.info.provided al crear INITIATED; es un evento distinto.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ModuleOneNotificationMatrixTest#expiredAndPaymentFailedNotifyNobody. No verifica todas las transiciones publicables ni sus filas outbox persistidas y entrega.

#### Traceability
FR-001, FR-006 | SC-001, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#initiatedAndPendingPaymentNotifyNobody; src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#expiredAndPaymentFailedNotifyNobody | In Process
