### Test Case Specification: TC-CU11-004 — Fallo de bloque en lote
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo de bloque en lote. Requisitos: FR-008. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
readTimeoutMarksBlockUnavailableWithSingleCall prueba timeout separado; no se verifica visualización del catálogo de SC-004.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 simulado falla un bloque de consulta en lote.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpBatchEstimateAdapterTest#failedBlockMakesItsIdsUnavailableAndKeepsGoing (XML V4: passed). Aserta 50 IDs no disponibles, 10 estimaciones y dos llamadas WireMock. readTimeoutMarksBlockUnavailableWithSingleCall prueba timeout separado; no se verifica visualización del catálogo de SC-004. |
| 2 | Ejecutar | Consultar lote y revisar resultado. | Datos de las precondiciones | Los IDs del bloque fallido quedan no disponibles; otros bloques se procesan sin reintento del bloque. | Pendiente | HttpBatchEstimateAdapterTest#failedBlockMakesItsIdsUnavailableAndKeepsGoing (XML V4: passed). Aserta 50 IDs no disponibles, 10 estimaciones y dos llamadas WireMock. readTimeoutMarksBlockUnavailableWithSingleCall prueba timeout separado; no se verifica visualización del catálogo de SC-004. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpBatchEstimateAdapterTest#failedBlockMakesItsIdsUnavailableAndKeepsGoing (XML V4: passed). Aserta 50 IDs no disponibles, 10 estimaciones y dos llamadas WireMock. readTimeoutMarksBlockUnavailableWithSingleCall prueba timeout separado; no se verifica visualización del catálogo de SC-004. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-01 implementa el catálogo backend; la navegación UI no se prueba aquí.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpBatchEstimateAdapterTest#failedBlockMakesItsIdsUnavailableAndKeepsGoing. readTimeoutMarksBlockUnavailableWithSingleCall prueba timeout separado; no se verifica visualización del catálogo de SC-004.

#### Traceability
FR-008 | SC-004 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#failedBlockMakesItsIdsUnavailableAndKeepsGoing; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#readTimeoutMarksBlockUnavailableWithSingleCall | In Process
