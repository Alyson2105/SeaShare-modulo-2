### Test Case Specification: TC-CU17-002 — Revisión en curso
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar revisión en curso. Requisitos: FR-001, FR-002, FR-007. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-17 y su endpoint aún no existen; los resultados son expectativas del contrato. El SLA de 24 horas requiere medición del flujo completo.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: Sin prueba de CU-17; no demuestran el flujo completo de CU-17.

##### Preconditions & Default Setup
Disputa PENDING con revisión administrativa sin terminar.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Actualizar a PENDING con nota interna. | Datos de las precondiciones | Se conserva PENDING y se registra actividad sin evento final a M3. | Pendiente | Contrato futuro |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El SLA de 24 horas requiere medición del flujo completo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Blocked; sin ejecución automatizada.

#### Traceability
FR-001, FR-002, FR-007 | SC-001 | Sin prueba de CU-17 | Blocked
