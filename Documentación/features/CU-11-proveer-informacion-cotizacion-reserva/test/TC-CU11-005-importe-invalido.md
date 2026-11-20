### Test Case Specification: TC-CU11-005 — Importe individual inválido
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar importe individual inválido. Requisitos: FR-015, FR-016. Criterios: SC-006.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
negativeTotalThrowsQuotationUnavailable y unparseableTotalThrowsQuotationUnavailable cubren variantes separadas; no se demuestra que se impida el paso a PENDING_PAYMENT de SC-006.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 responde estimación individual con importe nulo, ilegible, cero o negativo.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpIndividualEstimateAdapterTest#zeroTotalThrowsQuotationUnavailable (XML V4: passed). Aserta QUOTATION_UNAVAILABLE para importe cero con WireMock. negativeTotalThrowsQuotationUnavailable y unparseableTotalThrowsQuotationUnavailable cubren variantes separadas; no se demuestra que se impida el paso a PENDING_PAYMENT de SC-006. |
| 2 | Ejecutar | Solicitar estimación individual para cada variante. | Datos de las precondiciones | El adaptador rechaza con QUOTATION_UNAVAILABLE; no inventa un importe. | Pendiente | HttpIndividualEstimateAdapterTest#zeroTotalThrowsQuotationUnavailable (XML V4: passed). Aserta QUOTATION_UNAVAILABLE para importe cero con WireMock. negativeTotalThrowsQuotationUnavailable y unparseableTotalThrowsQuotationUnavailable cubren variantes separadas; no se demuestra que se impida el paso a PENDING_PAYMENT de SC-006. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpIndividualEstimateAdapterTest#zeroTotalThrowsQuotationUnavailable (XML V4: passed). Aserta QUOTATION_UNAVAILABLE para importe cero con WireMock. negativeTotalThrowsQuotationUnavailable y unparseableTotalThrowsQuotationUnavailable cubren variantes separadas; no se demuestra que se impida el paso a PENDING_PAYMENT de SC-006. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-02 no consume este puerto directamente; impedir reserva completa depende de CU-19 y CU-03.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpIndividualEstimateAdapterTest#zeroTotalThrowsQuotationUnavailable. negativeTotalThrowsQuotationUnavailable y unparseableTotalThrowsQuotationUnavailable cubren variantes separadas; no se demuestra que se impida el paso a PENDING_PAYMENT de SC-006.

#### Traceability
FR-015, FR-016 | SC-006 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#zeroTotalThrowsQuotationUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#negativeTotalThrowsQuotationUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#unparseableTotalThrowsQuotationUnavailable | In Process
