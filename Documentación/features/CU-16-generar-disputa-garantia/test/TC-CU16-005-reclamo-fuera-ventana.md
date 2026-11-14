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
La prueba de servicio con PostgreSQL comprueba WINDOW_EXPIRED y ausencia de reclamo en el límite; no comprueba todos los campos de la disputa ni el flujo HTTP real.

##### Dependencies & Assumptions
La evidencia V4 y sus límites se indican en Comments.

##### Preconditions & Default Setup
Disputa PENDING sin reclamo y reloj exactamente en window_end o posterior.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd (XML V4: passed). assertCode verifica WINDOW_EXPIRED; hasClaim es falso y la consulta de reclamo está vacía. El controlador HTTP se prueba aparte con un caso de uso simulado. |
| 2 | Ejecutar | Intentar registrar reclamo. | Datos de las precondiciones | El plan exige now < window_end y prevé 409 WINDOW_EXPIRED; no se acepta reclamo ni se altera la disputa por la solicitud. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd (XML V4: passed). assertCode verifica WINDOW_EXPIRED; hasClaim es falso y la consulta de reclamo está vacía. El controlador HTTP se prueba aparte con un caso de uso simulado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd (XML V4: passed). assertCode verifica WINDOW_EXPIRED; hasClaim es falso y la consulta de reclamo está vacía. El controlador HTTP se prueba aparte con un caso de uso simulado. |

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
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: RegisterDisputeClaimIntegrationTest#rejectsClaimAtExactWindowEnd. La prueba de servicio con PostgreSQL comprueba WINDOW_EXPIRED y ausencia de reclamo en el límite; no comprueba todos los campos de la disputa ni el flujo HTTP real.

#### Traceability
FR-005, FR-009 | SC-005 | src/test/java/com/seashare/m2/disputa/integration/RegisterDisputeClaimIntegrationTest.java#rejectsClaimAtExactWindowEnd | In Process
