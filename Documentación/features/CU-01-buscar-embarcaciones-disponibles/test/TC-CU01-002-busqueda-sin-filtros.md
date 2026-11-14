### Test Case Specification: TC-CU01-002 — Catálogo sin filtros
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU01-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar catálogo sin filtros. Requisitos: FR-001, FR-002, FR-009. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
MockMvc con proveedores en memoria verifica la primera página, no la totalidad de páginas del catálogo ni proveedores reales.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); no demuestran el flujo completo de CU-01.

##### Preconditions & Default Setup
Catálogo con varias embarcaciones y cotizaciones simuladas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty (XML V4: passed). Aserta HTTP 200, página 1, tamaño 20, 25 elementos totales y precio del primer resultado con proveedores simulados. |
| 2 | Ejecutar | Consultar GET /api/v1/vessels sin fechas ni pasajeros. | Datos de las precondiciones | El plan prevé 200 y catálogo completo paginado sin filtros de fecha ni capacidad. | Pendiente | VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty (XML V4: passed). Aserta HTTP 200, página 1, tamaño 20, 25 elementos totales y precio del primer resultado con proveedores simulados. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty (XML V4: passed). Aserta HTTP 200, página 1, tamaño 20, 25 elementos totales y precio del primer resultado con proveedores simulados. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
No equivale a garantizar disponibilidad del calendario ni estado operativo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: VesselSearchControllerTest#searchWithoutFiltersReturnsFirstPageOfTwenty. MockMvc con proveedores en memoria verifica la primera página, no la totalidad de páginas del catálogo ni proveedores reales.

#### Traceability
FR-001, FR-002, FR-009 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#searchWithoutFiltersReturnsFirstPageOfTwenty | In Process
