### Test Case Specification: TC-CU11-006 — Error de parámetros M3
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Individual](../plan.external.individual.md) · [Lote](../plan.external.lote.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU11-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar error de parámetros m3. Requisitos: FR-010, FR-011. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan individual | Contrato | [individual](../plan.external.individual.md) |
| Plan lote | Contrato | [lote](../plan.external.lote.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No comprueba que llegue el motivo concreto de M3 al usuario ni que se impida crear reserva.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 simulado responde 400 por parámetros o fechas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpIndividualEstimateAdapterTest#badRequest400ThrowsInvalidParameters (XML V4: passed). Aserta INVALID_PARAMETERS y una llamada al proveedor simulado ante HTTP 400. No comprueba que llegue el motivo concreto de M3 al usuario ni que se impida crear reserva. |
| 2 | Ejecutar | Solicitar estimación individual. | Datos de las precondiciones | El adaptador lanza INVALID_PARAMETERS sin reintentar. | Pendiente | HttpIndividualEstimateAdapterTest#badRequest400ThrowsInvalidParameters (XML V4: passed). Aserta INVALID_PARAMETERS y una llamada al proveedor simulado ante HTTP 400. No comprueba que llegue el motivo concreto de M3 al usuario ni que se impida crear reserva. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpIndividualEstimateAdapterTest#badRequest400ThrowsInvalidParameters (XML V4: passed). Aserta INVALID_PARAMETERS y una llamada al proveedor simulado ante HTTP 400. No comprueba que llegue el motivo concreto de M3 al usuario ni que se impida crear reserva. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
No conserva el mensaje específico de M3; CU-19 consume el puerto pero falta verificar ausencia de reserva en el flujo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpIndividualEstimateAdapterTest#badRequest400ThrowsInvalidParameters. No comprueba que llegue el motivo concreto de M3 al usuario ni que se impida crear reserva.

#### Traceability
FR-010, FR-011 | SC-005 | [individual](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java#badRequest400ThrowsInvalidParameters | In Process
