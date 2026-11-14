### Test Case Specification: TC-CU11-001 — Estimación individual
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estimación individual. Requisitos: FR-001, FR-002, FR-010, FR-012. Criterios: SC-001, SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No comprueba todos los parámetros ni el texto obligatorio o su presentación; el DTO no contiene duration_days y M3 real no se ejercita.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo seashare.finance.mode=http; M3 simulado devuelve importe positivo y warning textual.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpIndividualEstimateAdapterTest#successful200WithWarning (XML V4: passed). Aserta monto 150000.00 y warning arbitrario literal con WireMock. No comprueba todos los parámetros ni el texto obligatorio o su presentación; el DTO no contiene duration_days y M3 real no se ejercita. |
| 2 | Ejecutar | Solicitar estimación individual con embarcación, fechas y pasajeros. | Datos de las precondiciones | El adaptador envía parámetros y devuelve BigDecimal y warning sin cálculo monetario local. | Pendiente | HttpIndividualEstimateAdapterTest#successful200WithWarning (XML V4: passed). Aserta monto 150000.00 y warning arbitrario literal con WireMock. No comprueba todos los parámetros ni el texto obligatorio o su presentación; el DTO no contiene duration_days y M3 real no se ejercita. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpIndividualEstimateAdapterTest#successful200WithWarning (XML V4: passed). Aserta monto 150000.00 y warning arbitrario literal con WireMock. No comprueba todos los parámetros ni el texto obligatorio o su presentación; el DTO no contiene duration_days y M3 real no se ejercita. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El DTO no devuelve duration_days; warning ausente se acepta como null pese al spec.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpIndividualEstimateAdapterTest#successful200WithWarning. No comprueba todos los parámetros ni el texto obligatorio o su presentación; el DTO no contiene duration_days y M3 real no se ejercita.

#### Traceability
FR-001, FR-002, FR-010, FR-012 | SC-001, SC-002 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#successful200WithWarning | In Process
