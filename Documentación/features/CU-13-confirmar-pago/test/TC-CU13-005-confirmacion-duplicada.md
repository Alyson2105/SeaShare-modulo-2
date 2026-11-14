### Test Case Specification: TC-CU13-005 — Idempotencia de confirmación
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU13-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar idempotencia de confirmación. Requisitos: FR-007. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Plan externo | Contrato de consulta | [plan.external](../plan.external.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El caso exige repetir por ambos canales y respuesta HTTP del canal A; no existe su controlador de entrada.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Las pruebas citadas son parciales y no demuestran el flujo completo del caso.

##### Preconditions & Default Setup
Confirmación APPROVED ya procesada con reserva y referencia conocidas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ConfirmPaymentIntegrationTest#duplicateApprovalDoesNotRepeatTransitions (XML V4: passed). Aserta resultado DUPLICATE y verifica una llamada de evento RESERVED mediante espía de outbox. El caso exige repetir por ambos canales y respuesta HTTP del canal A; no existe su controlador de entrada. |
| 2 | Ejecutar | Repetir el mismo par external_transaction_id y result para la reserva por ambos canales. | Datos de las precondiciones | El plan exige éxito sin nueva transición ni duplicar efectos. | Pendiente | ConfirmPaymentIntegrationTest#duplicateApprovalDoesNotRepeatTransitions (XML V4: passed). Aserta resultado DUPLICATE y verifica una llamada de evento RESERVED mediante espía de outbox. El caso exige repetir por ambos canales y respuesta HTTP del canal A; no existe su controlador de entrada. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ConfirmPaymentIntegrationTest#duplicateApprovalDoesNotRepeatTransitions (XML V4: passed). Aserta resultado DUPLICATE y verifica una llamada de evento RESERVED mediante espía de outbox. El caso exige repetir por ambos canales y respuesta HTTP del canal A; no existe su controlador de entrada. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La idempotencia de creación CU-02 no cubre confirmación; canal A y B comparten clave propuesta.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Blocked; estado documental: Blocked; evidencia: ConfirmPaymentIntegrationTest#duplicateApprovalDoesNotRepeatTransitions. El caso exige repetir por ambos canales y respuesta HTTP del canal A; no existe su controlador de entrada.

#### Traceability
FR-007 | SC-004 | Lógica de confirmación probada | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#duplicateApprovalDoesNotRepeatTransitions | Blocked
