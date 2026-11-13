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
La lógica de CU-13 y la consulta periódica existen; el endpoint de canal A no se encontró. La idempotencia de creación CU-02 no cubre confirmación; canal A y B comparten clave propuesta.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: Sin prueba de CU-13; no demuestran el flujo completo de CU-13.

##### Preconditions & Default Setup
Confirmación APPROVED ya procesada con reserva y referencia conocidas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Repetir el mismo par external_transaction_id y result para la reserva por ambos canales. | Datos de las precondiciones | El plan exige éxito sin nueva transición ni duplicar efectos. | Pendiente | Integración: ConfirmPaymentIntegrationTest#duplicateApprovalDoesNotRepeatTransitions pasó para lógica interna y un evento RESERVED. Falta el canal A requerido por el caso y su respuesta HTTP. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

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
Ejecutado el 2026-11-13: Blocked; evidencia parcial, consultar el flujo.

#### Traceability
FR-007 | SC-004 | Lógica de confirmación probada | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#duplicateApprovalDoesNotRepeatTransitions | Blocked
