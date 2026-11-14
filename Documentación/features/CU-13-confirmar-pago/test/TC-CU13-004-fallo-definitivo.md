### Test Case Specification: TC-CU13-004 — Fallo definitivo
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU13-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo definitivo. Requisitos: FR-005. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Plan externo | Contrato de consulta | [plan.external](../plan.external.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-13: FAILED se probó en el servicio local con PostgreSQL. El canal HTTP A sigue ausente y la integración real con M3 está aplazada.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); no demuestran el flujo completo de CU-13.

##### Preconditions & Default Setup
Reserva PENDING_PAYMENT con resultado FAILED definitivo.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ConfirmPaymentIntegrationTest#cancelledChargeFailsTheReservation (XML V4: passed). Aserta PAYMENT_FAILED en resultado y reserva ante CANCELLED de M3. El caso requiere FAILED, pero PaymentStatus no contiene FAILED y el canal A HTTP no está implementado; CANCELLED es otra entrada probada. Falta verificar ausencia de notificación M1. |
| 2 | Ejecutar | Procesar resultado. | Datos de las precondiciones | Según spec se transiciona a PAYMENT_FAILED sin notificación M1. | Pendiente | ConfirmPaymentIntegrationTest#cancelledChargeFailsTheReservation (XML V4: passed). Aserta PAYMENT_FAILED en resultado y reserva ante CANCELLED de M3. El caso requiere FAILED, pero PaymentStatus no contiene FAILED y el canal A HTTP no está implementado; CANCELLED es otra entrada probada. Falta verificar ausencia de notificación M1. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ConfirmPaymentIntegrationTest#cancelledChargeFailsTheReservation (XML V4: passed). Aserta PAYMENT_FAILED en resultado y reserva ante CANCELLED de M3. El caso requiere FAILED, pero PaymentStatus no contiene FAILED y el canal A HTTP no está implementado; CANCELLED es otra entrada probada. Falta verificar ausencia de notificación M1. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La confirmación interna simulada ante FAILED está probada; el canal A sigue sin implementación.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Blocked; estado documental: Blocked; evidencia: ConfirmPaymentIntegrationTest#cancelledChargeFailsTheReservation. El caso requiere FAILED, pero PaymentStatus no contiene FAILED y el canal A HTTP no está implementado; CANCELLED es otra entrada probada. Falta verificar ausencia de notificación M1.

#### Traceability
FR-005 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#cancelledChargeFailsTheReservation; src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#expiredAndPaymentFailedNotifyNobody | Blocked

#### Evidencia local posterior a V4 — 2026-11-13
ConfirmPaymentIntegrationTest#failedConfirmationDoesNotNotifyM1OrRepeatEffects; confirmación simulada, una transición persistida y ninguna nueva fila M1_HTTP; corrida focalizada 2026-11-13, 18 pruebas sin fallos. Veredicto técnico: Partial. No acredita integración real M1/M3 ni resuelve las decisiones contractuales pendientes.
