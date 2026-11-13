### Test Case Specification: TC-CU20-006 — Subestado de cancelación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU20-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar subestado de cancelación. Requisitos: FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La identidad actual usa headers X-User-Id y X-User-Role, sin Spring Security. La respuesta del listado expone cancellation_subtype; faltan pruebas de otros subtipos y del importe.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: Sin prueba de CU-20; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva propia CANCELLED con subtipo conocido.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar lista y verificar subtipo. | Datos de las precondiciones | El spec exige incluir subestado cualitativo sin alterar importe. | Pendiente | ReservationQueryTest#listsWithAppliedFilter: exitosa. Comprueba subtipo FLEXIBLE y etiqueta contextual; no compara el importe ni otros subtipos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La respuesta del listado expone cancellation_subtype; faltan pruebas de otros subtipos y del importe.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; Comprueba subtipo FLEXIBLE y etiqueta contextual; no compara el importe ni otros subtipos.

#### Traceability
FR-005 | SC-001 | Prueba parcial ejecutada  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsWithAppliedFilter | In Process
