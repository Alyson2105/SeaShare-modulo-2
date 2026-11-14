### Test Case Specification: TC-CU15-003 — Consulta de reserva completada
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU15-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consulta de reserva completada. Requisitos: FR-003, FR-006. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
EndNavigationServiceTest prueba escritura de notas con mocks, no una consulta GET de reserva completada; no se identificó ejecución automatizada pertinente a la lectura exigida.

##### Dependencies & Assumptions
EndNavigationServiceTest comprueba escritura de novedades y check-out mediante puertos simulados; no comprueba el GET de CU-15.

##### Preconditions & Default Setup
Reserva COMPLETED con hora real y texto de novedades registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente. EndNavigationServiceTest prueba escritura de notas con mocks, no una consulta GET de reserva completada; no se identificó ejecución automatizada pertinente a la lectura exigida. |
| 2 | Ejecutar | Consultar la reserva como M3. | Datos de las precondiciones | El plan prevé checkout y texto literal sin valorar daños monetariamente. | Pendiente | Sin ejecución automatizada pertinente. EndNavigationServiceTest prueba escritura de notas con mocks, no una consulta GET de reserva completada; no se identificó ejecución automatizada pertinente a la lectura exigida. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente. EndNavigationServiceTest prueba escritura de notas con mocks, no una consulta GET de reserva completada; no se identificó ejecución automatizada pertinente a la lectura exigida. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-07 implementa check-out; no se ha probado la consulta integrada de una reserva completada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Pendiente; sin ejecución automatizada pertinente. EndNavigationServiceTest prueba escritura de notas con mocks, no una consulta GET de reserva completada; no se identificó ejecución automatizada pertinente a la lectura exigida.

#### Traceability
FR-003, FR-006 | SC-003 | Antecedentes de escritura, no evidencia de consulta: src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#remarksArePassedAsCompletionNotesAndChangeMessage; src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#writesReservationThenCallsCu08ThenInsertsCheckout | In Process
