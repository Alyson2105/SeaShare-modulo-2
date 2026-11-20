### Test Case Specification: TC-CU13-002 — Aprobación tras TTL
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU13-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar aprobación tras ttl. Requisitos: FR-003, FR-006. Criterios: SC-002, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Plan externo | Contrato de consulta | [plan.external](../plan.external.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No demuestra entrega real a M3 ni respuesta HTTP de canal A.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-13.

##### Preconditions & Default Setup
Reserva EXPIRED por TTL y resultado APPROVED posterior.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ConfirmPaymentIntegrationTest#approvedOnExpiredReservationPublishesReversal (XML V4: passed). Aserta LATE_REVERSAL y EXPIRED persistido; verifica con espía una solicitud de reversión y ausencia de evento RESERVED. No demuestra entrega real a M3 ni respuesta HTTP de canal A. |
| 2 | Ejecutar | Procesar confirmación y observar instrucción de reversión. | Datos de las precondiciones | Plan exige no pasar a RESERVED e instruir reversión: canal A prevé 409 y reversal_required; canal B evento CANCELLED_FLEXIBLE. | Pendiente | ConfirmPaymentIntegrationTest#approvedOnExpiredReservationPublishesReversal (XML V4: passed). Aserta LATE_REVERSAL y EXPIRED persistido; verifica con espía una solicitud de reversión y ausencia de evento RESERVED. No demuestra entrega real a M3 ni respuesta HTTP de canal A. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ConfirmPaymentIntegrationTest#approvedOnExpiredReservationPublishesReversal (XML V4: passed). Aserta LATE_REVERSAL y EXPIRED persistido; verifica con espía una solicitud de reversión y ausencia de evento RESERVED. No demuestra entrega real a M3 ni respuesta HTTP de canal A. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Corte exacto sin gracia; mecanismo de detección tardía del canal B requiere aclaración.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ConfirmPaymentIntegrationTest#approvedOnExpiredReservationPublishesReversal. No demuestra entrega real a M3 ni respuesta HTTP de canal A.

#### Traceability
FR-003, FR-006 | SC-002, SC-003 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#lateApprovalPublishesCancelledFlexibleOnceAndKeepsState; src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#approvedOnExpiredReservationPublishesReversal | In Process
