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
Existen flujos de cancelación e inasistencia; no se verificó ausencia de disputa después del cierre.

##### Dependencies & Assumptions
El escenario requiere CU-04 o CU-05 y consulta de disputas; no se identificó prueba pertinente.

##### Preconditions & Default Setup
Reserva que finaliza en CANCELLED por cancelación o inasistencia.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin prueba ejecutada que consulte disputas tras cancelación o inasistencia. |
| 2 | Ejecutar | Consolidar cierre y consultar disputas asociadas. | Datos de las precondiciones | No se crea disputa de garantía. | Pendiente | Sin prueba ejecutada que consulte disputas tras cancelación o inasistencia. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin prueba ejecutada que consulte disputas tras cancelación o inasistencia. |

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
Evaluado el 2026-11-13: Pendiente; sin ejecución automatizada pertinente.

#### Traceability
FR-002 | SC-002 | Sin prueba ejecutada pertinente | In Process
