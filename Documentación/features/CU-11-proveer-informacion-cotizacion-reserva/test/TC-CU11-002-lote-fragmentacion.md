### Test Case Specification: TC-CU11-002 — Fragmentación de lote
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fragmentación de lote. Requisitos: FR-003, FR-004, FR-005. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La precondición exige 120 UUID más duplicados; la prueba usa 60 sin duplicados y no inspecciona tamaños de los cuerpos. duplicateIdsAreDeduplicatedBeforeSend prueba deduplicación por separado.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo HTTP y 120 UUID distintos más duplicados.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpBatchEstimateAdapterTest#sixtyIdsSplitIntoTwoBlocksOfFifty (XML V4: passed). Aserta 60 estimaciones, lista de no disponibles vacía y dos llamadas WireMock. La precondición exige 120 UUID más duplicados; la prueba usa 60 sin duplicados y no inspecciona tamaños de los cuerpos. duplicateIdsAreDeduplicatedBeforeSend prueba deduplicación por separado. |
| 2 | Ejecutar | Solicitar estimaciones y observar solicitudes a M3. | Datos de las precondiciones | Se deduplican IDs, se envían bloques de hasta 50 y se consolidan resultados. | Pendiente | HttpBatchEstimateAdapterTest#sixtyIdsSplitIntoTwoBlocksOfFifty (XML V4: passed). Aserta 60 estimaciones, lista de no disponibles vacía y dos llamadas WireMock. La precondición exige 120 UUID más duplicados; la prueba usa 60 sin duplicados y no inspecciona tamaños de los cuerpos. duplicateIdsAreDeduplicatedBeforeSend prueba deduplicación por separado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpBatchEstimateAdapterTest#sixtyIdsSplitIntoTwoBlocksOfFifty (XML V4: passed). Aserta 60 estimaciones, lista de no disponibles vacía y dos llamadas WireMock. La precondición exige 120 UUID más duplicados; la prueba usa 60 sin duplicados y no inspecciona tamaños de los cuerpos. duplicateIdsAreDeduplicatedBeforeSend prueba deduplicación por separado. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Lista vacía retorna resultado vacío sin llamada; el cálculo del importe pertenece a M3.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpBatchEstimateAdapterTest#sixtyIdsSplitIntoTwoBlocksOfFifty. La precondición exige 120 UUID más duplicados; la prueba usa 60 sin duplicados y no inspecciona tamaños de los cuerpos. duplicateIdsAreDeduplicatedBeforeSend prueba deduplicación por separado.

#### Traceability
FR-003, FR-004, FR-005 | SC-003 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#duplicateIdsAreDeduplicatedBeforeSend; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#sixtyIdsSplitIntoTwoBlocksOfFifty | In Process
