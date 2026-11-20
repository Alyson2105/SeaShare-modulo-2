### Test Case Specification: TC-CU11-007 — Recotización al cambiar viaje
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar recotización al cambiar viaje. Requisitos: FR-009, FR-014. Criterios: SC-007.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La recotización y descarte de respuestas obsoletas al cambiar parámetros de CU-19 requieren flujo de interfaz no implementado; sin ejecución automatizada pertinente.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: Sin prueba de CU-11; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Estimación individual previa y nueva selección de fechas o pasajeros.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente. La recotización y descarte de respuestas obsoletas al cambiar parámetros de CU-19 requieren flujo de interfaz no implementado; sin ejecución automatizada pertinente. |
| 2 | Ejecutar | Cambiar parámetros en CU-19 y observar nueva consulta. | Datos de las precondiciones | El spec exige descartar la estimación anterior y solicitar otra con los nuevos parámetros. | Pendiente | Sin ejecución automatizada pertinente. La recotización y descarte de respuestas obsoletas al cambiar parámetros de CU-19 requieren flujo de interfaz no implementado; sin ejecución automatizada pertinente. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente. La recotización y descarte de respuestas obsoletas al cambiar parámetros de CU-19 requieren flujo de interfaz no implementado; sin ejecución automatizada pertinente. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-19 implementa la consulta individual; la lógica UI de recotización y descarte de respuestas obsoletas no está implementada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Blocked; sin ejecución automatizada pertinente. La recotización y descarte de respuestas obsoletas al cambiar parámetros de CU-19 requieren flujo de interfaz no implementado; sin ejecución automatizada pertinente.

#### Traceability
FR-009, FR-014 | SC-007 | Sin prueba de CU-11 | Blocked
