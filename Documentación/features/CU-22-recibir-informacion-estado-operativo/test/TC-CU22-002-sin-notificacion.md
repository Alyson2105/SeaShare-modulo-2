### Test Case Specification: TC-CU22-002 — Estados sin notificación M1
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU22-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estados sin notificación m1. Requisitos: FR-001, FR-002. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-22 encola destino M1_HTTP; el adaptador actual es fake y no realiza HTTP real. CU-02 sí encola evento AMQP de información a M3 al crear; es distinto.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas en INITIATED y PENDING_PAYMENT con transiciones válidas a EXPIRED o PAYMENT_FAILED.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Inspeccionar notificaciones M1 de creación, pago pendiente, expiración y fallo. | Datos de las precondiciones | No se encola M1_HTTP en esas etapas. | Pendiente | ChangeReservationStatusServiceTest.java);; ModuleNotificationMatrixTest.java); ModuleNotificationMatrixTest#initiatedAndPendingPaymentNotifyNobody; ModuleNotificationMatrixTest#expiredAndPaymentFailedNotifyNobody: exitosa. Matriz unitaria comprueba ausencia de destino M1; no comprueba cada transición en outbox real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-02 sí encola evento AMQP de información a M3 al crear; es distinto.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; Matriz unitaria comprueba ausencia de destino M1; no comprueba cada transición en outbox real.

#### Traceability
FR-001, FR-002 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java)  | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#initiatedAndPendingPaymentNotifyNobody | src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#expiredAndPaymentFailedNotifyNobody | In Process
