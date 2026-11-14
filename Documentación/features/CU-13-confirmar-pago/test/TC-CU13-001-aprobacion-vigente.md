### Test Case Specification: TC-CU13-001 — Aprobación con TTL vigente
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU13-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar aprobación con ttl vigente. Requisitos: FR-002, FR-003, FR-004. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Plan externo | Contrato de consulta | [plan.external](../plan.external.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No comprueba marca temporal, cancelación del TTL, plazo inferior a un segundo ni recorrido completo de canal externo.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-13.

##### Preconditions & Default Setup
Reserva PENDING_PAYMENT con TTL vigente y resultado APPROVED con referencia válida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ConfirmPaymentIntegrationTest#approvedWithinTtlReservesWithPaymentReference (XML V4: passed). Aserta CONFIRMED, estado RESERVED y referencia pg-8841 persistidos; verifica evento RESERVED mediante espía de outbox. No comprueba marca temporal, cancelación del TTL, plazo inferior a un segundo ni recorrido completo de canal externo. |
| 2 | Ejecutar | Procesar confirmación mediante canal habilitado. | Datos de las precondiciones | Según el plan, reserva pasa a RESERVED por CU-08 y registra referencia y hora de pago. | Pendiente | ConfirmPaymentIntegrationTest#approvedWithinTtlReservesWithPaymentReference (XML V4: passed). Aserta CONFIRMED, estado RESERVED y referencia pg-8841 persistidos; verifica evento RESERVED mediante espía de outbox. No comprueba marca temporal, cancelación del TTL, plazo inferior a un segundo ni recorrido completo de canal externo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ConfirmPaymentIntegrationTest#approvedWithinTtlReservesWithPaymentReference (XML V4: passed). Aserta CONFIRMED, estado RESERVED y referencia pg-8841 persistidos; verifica evento RESERVED mediante espía de outbox. No comprueba marca temporal, cancelación del TTL, plazo inferior a un segundo ni recorrido completo de canal externo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La lógica de confirmación y el canal B existen; el canal A no se encontró.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ConfirmPaymentIntegrationTest#approvedWithinTtlReservesWithPaymentReference. No comprueba marca temporal, cancelación del TTL, plazo inferior a un segundo ni recorrido completo de canal externo.

#### Traceability
FR-002, FR-003, FR-004 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#approvedWithinTtlReservesWithPaymentReference | In Process
