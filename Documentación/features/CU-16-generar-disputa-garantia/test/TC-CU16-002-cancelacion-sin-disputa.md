### Test Case Specification: TC-CU16-002 — Reserva cancelada sin disputa
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reserva cancelada sin disputa. Requisitos: FR-002. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La implementación de cancelación e inasistencia existe; no se encontró prueba que consulte las disputas después de ambos flujos.

##### Dependencies & Assumptions
La evidencia V4 y sus límites se indican en Comments.

##### Preconditions & Default Setup
Reserva que finaliza en CANCELLED por cancelación o inasistencia.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente que verifique la ausencia de disputa después de cancelación e inasistencia. |
| 2 | Ejecutar | Consolidar cierre y consultar disputas asociadas. | Datos de las precondiciones | No se crea disputa de garantía. | Pendiente | Sin ejecución automatizada pertinente que verifique la ausencia de disputa después de cancelación e inasistencia. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente que verifique la ausencia de disputa después de cancelación e inasistencia. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Requiere CU-04 o CU-05 y contexto disputa.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Pendiente; sin ejecución automatizada pertinente. La implementación de cancelación e inasistencia existe; no se encontró prueba que consulte las disputas después de ambos flujos.

#### Traceability
FR-002 | SC-002 | Sin prueba ejecutada pertinente | In Process
