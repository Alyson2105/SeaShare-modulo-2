### Test Case Specification: TC-CU19-003 — Cotización de detalle
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU19-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar cotización de detalle. Requisitos: FR-005, FR-006. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Endpoints de detalle y cotización implementados; cobertura parcial con dobles, sin verificar servicios reales ni UI. MockMvc verifica importe literal y tres días con adaptadores falsos; plan exige quotation_id y vencimiento que CU-11 indica que M3 no devuelve.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); no demuestran el flujo completo de CU-19.

##### Preconditions & Default Setup
Datos de embarcación, fechas y pasajeros válidos; M3 simulado devuelve cotización.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Solicitar cotización de detalle. | Datos de las precondiciones | Plan prevé 200 con total y advertencia de M3 sin cálculos por noche. | Pendiente | MockMvc verifica importe literal y tres días con adaptadores falsos; plan exige quotation_id y vencimiento que CU-11 indica que M3 no devuelve. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Plan incluye quotation_id y expiración que CU-11 indica que M3 no devuelve; aclarar contrato.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Draft; MockMvc verifica importe literal y tres días con adaptadores falsos; plan exige quotation_id y vencimiento que CU-11 indica que M3 no devuelve.

#### Traceability
FR-005, FR-006 | SC-001 | [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java)  | src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java#quotationReturnsLiteralValuesAndInclusiveDays | Draft
