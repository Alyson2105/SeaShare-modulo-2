### Test Case Specification: TC-CU12-006 — Auditoría de cálculo
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU12-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar auditoría de cálculo. Requisitos: FR-010. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
FR-010 exige asiento con reservation_id, montos y fecha; HttpCalculatedValueAdapter no registra solicitud ni respuesta y no se encontró prueba pertinente.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: Sin prueba de CU-12; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Solicitud de cálculo con respuesta de M3.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente. FR-010 exige asiento con reservation_id, montos y fecha; HttpCalculatedValueAdapter no registra solicitud ni respuesta y no se encontró prueba pertinente. |
| 2 | Ejecutar | Solicitar cálculo y consultar registro auditable. | Datos de las precondiciones | El spec exige registrar reservation_id, montos y fecha. | Pendiente | Sin ejecución automatizada pertinente. FR-010 exige asiento con reservation_id, montos y fecha; HttpCalculatedValueAdapter no registra solicitud ni respuesta y no se encontró prueba pertinente. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente. FR-010 exige asiento con reservation_id, montos y fecha; HttpCalculatedValueAdapter no registra solicitud ni respuesta y no se encontró prueba pertinente. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Auditoría FR-010 no implementada; falta definir persistencia del asiento.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Blocked; sin ejecución automatizada pertinente. FR-010 exige asiento con reservation_id, montos y fecha; HttpCalculatedValueAdapter no registra solicitud ni respuesta y no se encontró prueba pertinente.

#### Traceability
FR-010 | SC-001 | Sin prueba de CU-12 | Blocked
