### Test Case Specification: TC-CU19-004 — Fallo de cotización
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU19-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo de cotización. Requisitos: FR-005, FR-007. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Endpoints de detalle y cotización implementados; cobertura parcial con dobles, sin verificar servicios reales ni UI. Prueba unitaria verifica propagación de QUOTATION_UNAVAILABLE; respuesta HTTP y efectos no verificados.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); no demuestran el flujo completo de CU-19.

##### Preconditions & Default Setup
M3 simulado falla para una solicitud válida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar cotización de detalle. | Datos de las precondiciones | Según el plan, 503 QUOTATION_UNAVAILABLE; no se produce una cotización exitosa. | Pendiente | Prueba unitaria verifica propagación de QUOTATION_UNAVAILABLE; respuesta HTTP y efectos no verificados. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Mantener ficha y deshabilitar botón son pruebas UI; CU-02 actualmente no comprueba origen del monto.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; Prueba unitaria verifica propagación de QUOTATION_UNAVAILABLE; respuesta HTTP y efectos no verificados.

#### Traceability
FR-005, FR-007 | SC-001 | [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java)  | src/test/java/com/seashare/m2/reservas/application/service/VesselDetailServiceTest.java#financeFailurePropagates | In Process
