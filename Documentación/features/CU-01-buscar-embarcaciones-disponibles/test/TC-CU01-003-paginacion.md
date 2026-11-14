### Test Case Specification: TC-CU01-003 — Página de catálogo
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU01-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar página de catálogo. Requisitos: FR-003, FR-008. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Se verifica la primera página de 20 en un catálogo de 25; no se observan los identificadores enviados a cotización.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); no demuestran el flujo completo de CU-01.

##### Preconditions & Default Setup
Catálogo con más de 20 resultados y cotización lote observable.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty (XML V4: passed). Aserta tamaño 20, total 25 y dos páginas; el fake de cotización no registra los IDs recibidos. |
| 2 | Ejecutar | Consultar página y observar identificadores enviados a M3. | Datos de las precondiciones | El plan prevé página de hasta 20 y cotización solo de IDs visibles. | Pendiente | VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty (XML V4: passed). Aserta tamaño 20, total 25 y dos páginas; el fake de cotización no registra los IDs recibidos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty (XML V4: passed). Aserta tamaño 20, total 25 y dos páginas; el fake de cotización no registra los IDs recibidos. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Paginación de catálogo pertenece a CU-01, no al adaptador de lote aislado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty. Se verifica la primera página de 20 en un catálogo de 25; no se observan los identificadores enviados a cotización.

#### Traceability
FR-003, FR-008 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#searchWithoutFiltersReturnsFirstPageOfTwenty | In Process
