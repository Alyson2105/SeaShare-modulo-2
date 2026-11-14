### Test Case Specification: TC-CU11-008 — Advertencia en interfaz
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-008

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar advertencia en interfaz. Requisitos: FR-012, FR-013. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
successful200WithoutWarning aserta que se acepta warning null; el caso exige presentación literal de un warning suministrado por M3 en CU-19 y modal CU-02, sin prueba de UI.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 devuelve advertencia y monto para una selección válida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpIndividualEstimateAdapterTest#successful200WithWarning (XML V4: passed). Aserta que se conserva el warning arbitrario Credit risk review pending con importe 150000.00. successful200WithoutWarning aserta que se acepta warning null; el caso exige presentación literal de un warning suministrado por M3 en CU-19 y modal CU-02, sin prueba de UI. |
| 2 | Ejecutar | Solicitar estimación y verificar presentación en CU-19 y modal CU-02. | Datos de las precondiciones | El spec exige mostrar exactamente la advertencia. | Pendiente | HttpIndividualEstimateAdapterTest#successful200WithWarning (XML V4: passed). Aserta que se conserva el warning arbitrario Credit risk review pending con importe 150000.00. successful200WithoutWarning aserta que se acepta warning null; el caso exige presentación literal de un warning suministrado por M3 en CU-19 y modal CU-02, sin prueba de UI. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpIndividualEstimateAdapterTest#successful200WithWarning (XML V4: passed). Aserta que se conserva el warning arbitrario Credit risk review pending con importe 150000.00. successful200WithoutWarning aserta que se acepta warning null; el caso exige presentación literal de un warning suministrado por M3 en CU-19 y modal CU-02, sin prueba de UI. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Adaptador preserva texto presente pero acepta warning ausente; CU-19 lo transmite y la UI queda fuera.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpIndividualEstimateAdapterTest#successful200WithWarning. successful200WithoutWarning aserta que se acepta warning null; el caso exige presentación literal de un warning suministrado por M3 en CU-19 y modal CU-02, sin prueba de UI.

#### Traceability
FR-012, FR-013 | SC-002 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#successful200WithWarning; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#successful200WithoutWarning; src/test/java/com/seashare/m2/reservas/application/service/VesselDetailServiceTest.java#quotationUsesLiteralAmountAndInclusiveDays | In Process
