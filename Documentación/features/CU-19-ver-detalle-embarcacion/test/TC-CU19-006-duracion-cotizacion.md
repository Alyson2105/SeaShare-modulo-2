### Test Case Specification: TC-CU19-006 — Duración de un día
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU19-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar duración de un día. Requisitos: FR-005, FR-006. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
plan.md CU-19, Manejo de Errores del Endpoint 2, rechaza end_at igual a start_at; spec.md CU-11 User Story 2 admite un día inclusivo con fechas iguales. No se encontró ejecución del caso.

##### Dependencies & Assumptions
Sin ejecución automatizada pertinente para fechas iguales; el resultado contractual requiere aclaración.

##### Preconditions & Default Setup
Fecha inicial y final coinciden.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente. plan.md CU-19, Manejo de Errores del Endpoint 2, rechaza end_at igual a start_at; spec.md CU-11 User Story 2 admite un día inclusivo con fechas iguales. No se encontró ejecución del caso. |
| 2 | Ejecutar | Solicitar cotización individual para un día. | Datos de las precondiciones | El contrato CU-11 acepta un día inclusivo; el plan CU-19 parece rechazar fechas iguales. | Pendiente | Sin ejecución automatizada pertinente. plan.md CU-19, Manejo de Errores del Endpoint 2, rechaza end_at igual a start_at; spec.md CU-11 User Story 2 admite un día inclusivo con fechas iguales. No se encontró ejecución del caso. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente. plan.md CU-19, Manejo de Errores del Endpoint 2, rechaza end_at igual a start_at; spec.md CU-11 User Story 2 admite un día inclusivo con fechas iguales. No se encontró ejecución del caso. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Debe aclararse la regla y el cálculo de total_days antes de cerrar el contrato.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Draft; sin ejecución automatizada pertinente. plan.md CU-19, Manejo de Errores del Endpoint 2, rechaza end_at igual a start_at; spec.md CU-11 User Story 2 admite un día inclusivo con fechas iguales. No se encontró ejecución del caso.

#### Traceability
FR-005, FR-006 | SC-001 | Sin prueba de CU-19  | Draft

#### Decisión aprobada — 2026-11-13
Se aprobaron fechas iguales como un día inclusivo; la semántica de instantes, si se introduce, requiere contrato separado. Esta decisión no modifica automáticamente el veredicto del TC.
