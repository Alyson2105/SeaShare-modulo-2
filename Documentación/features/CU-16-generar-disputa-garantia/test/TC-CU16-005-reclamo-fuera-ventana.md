### Test Case Specification: TC-CU16-005 — Reclamo fuera de ventana
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reclamo fuera de ventana. Requisitos: FR-005, FR-009. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El endpoint y la regla temporal existen. El job puede ejecutarse con retraso: no asumir que ya se consolidó REJECTED.

##### Dependencies & Assumptions
La prueba usa componentes controlados; no demuestra el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING sin reclamo y reloj exactamente en window_end o posterior.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd: verde con PostgreSQL; comprueba rechazo y ausencia de reclamo, no el estado completo ni HTTP real. |
| 2 | Ejecutar | Intentar registrar reclamo. | Datos de las precondiciones | El plan exige now < window_end y prevé 409 WINDOW_EXPIRED; no se acepta reclamo ni se altera la disputa por la solicitud. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd: verde con PostgreSQL; comprueba rechazo y ausencia de reclamo, no el estado completo ni HTTP real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd: verde con PostgreSQL; comprueba rechazo y ausencia de reclamo, no el estado completo ni HTTP real. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El job puede ejecutarse con retraso: no asumir que ya se consolidó REJECTED.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial, pasos completos pendientes.

#### Traceability
FR-005, FR-009 | SC-005 | src/test/java/com/seashare/m2/disputa/integration/RegisterDisputeClaimIntegrationTest.java#rejectsClaimAtExactWindowEnd | In Process
