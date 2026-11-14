### Test Case Specification: TC-CU11-003 — Estimación parcial
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estimación parcial. Requisitos: FR-006, FR-007. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El spec, User Story 1 y FR-006, excluye seguro del lote; el Independent Test de la misma historia y plan.external.lote.md lo incluyen. La composición contractual del importe no es inequívoca.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Lote con estimaciones válidas y embarcaciones ausentes o sin tarifa.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpBatchEstimateAdapterTest#batch200PartialMarksUnavailable (XML V4: passed). Aserta una estimación y un ID no disponible con WireMock. El spec, User Story 1 y FR-006, excluye seguro del lote; el Independent Test de la misma historia y plan.external.lote.md lo incluyen. La composición contractual del importe no es inequívoca. |
| 2 | Ejecutar | Consultar lote. | Datos de las precondiciones | Conserva montos válidos y marca como no disponibles IDs ausentes, rechazados o con importe inválido. | Pendiente | HttpBatchEstimateAdapterTest#batch200PartialMarksUnavailable (XML V4: passed). Aserta una estimación y un ID no disponible con WireMock. El spec, User Story 1 y FR-006, excluye seguro del lote; el Independent Test de la misma historia y plan.external.lote.md lo incluyen. La composición contractual del importe no es inequívoca. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpBatchEstimateAdapterTest#batch200PartialMarksUnavailable (XML V4: passed). Aserta una estimación y un ID no disponible con WireMock. El spec, User Story 1 y FR-006, excluye seguro del lote; el Independent Test de la misma historia y plan.external.lote.md lo incluyen. La composición contractual del importe no es inequívoca. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La spec FR-006 excluye el seguro del importe de lote; su prueba independiente y plan.external.lote.md lo incluyen.

El catálogo de CU-01 está implementado; no se prueba la presentación UI.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: HttpBatchEstimateAdapterTest#batch200PartialMarksUnavailable. El spec, User Story 1 y FR-006, excluye seguro del lote; el Independent Test de la misma historia y plan.external.lote.md lo incluyen. La composición contractual del importe no es inequívoca.

#### Traceability
FR-006, FR-007 | SC-004 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#batch200PartialMarksUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#unknownIdInResponseBecomesUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java#invalidEstimatedTotalMakesVesselUnavailable | Draft
