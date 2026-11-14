### Test Case Specification: TC-CU20-005 — Filtro de pestañas
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU20-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar filtro de pestañas. Requisitos: FR-007, FR-012. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No cubre todos los estados ni la interfaz; SC-002 no está completamente demostrado.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas propias en estados activos y cancelados.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#listsWithAppliedFilter (XML V4: passed). Aserta filtros history y cancelled, contadores y estado CANCELLED. No cubre todos los estados ni la interfaz; SC-002 no está completamente demostrado. |
| 2 | Ejecutar | Solicitar filter=history y filter=cancelled. | Datos de las precondiciones | El plan espera subconjuntos distintos por filtro y metadatos correspondientes. | Pendiente | ReservationQueryTest#listsWithAppliedFilter (XML V4: passed). Aserta filtros history y cancelled, contadores y estado CANCELLED. No cubre todos los estados ni la interfaz; SC-002 no está completamente demostrado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#listsWithAppliedFilter (XML V4: passed). Aserta filtros history y cancelled, contadores y estado CANCELLED. No cubre todos los estados ni la interfaz; SC-002 no está completamente demostrado. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El filtro está implementado; faltan pruebas de todos los estados y de UI.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ReservationQueryTest#listsWithAppliedFilter. No cubre todos los estados ni la interfaz; SC-002 no está completamente demostrado.

#### Traceability
FR-007, FR-012 | SC-002 | Prueba parcial ejecutada  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#listsWithAppliedFilter | In Process
