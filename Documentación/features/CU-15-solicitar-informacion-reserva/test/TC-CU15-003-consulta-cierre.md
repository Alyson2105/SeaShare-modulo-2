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
CU-15 expone un endpoint interno con identidad temporal por headers; JWT de servicio está pendiente. CU-07 implementa check-out; no se ha probado la consulta integrada de una reserva completada.

##### Dependencies & Assumptions
EndNavigationServiceTest comprueba escritura de novedades y check-out mediante puertos simulados; no comprueba el GET de CU-15.

##### Preconditions & Default Setup
Reserva COMPLETED con hora real y texto de novedades registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar la reserva como M3. | Datos de las precondiciones | El plan prevé checkout y texto literal sin valorar daños monetariamente. | Pendiente | EndNavigationServiceTest#remarksArePassedAsCompletionNotesAndChangeMessage pasó con puertos simulados; comprueba la escritura, no la respuesta GET. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

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
Corrida 2026-11-13: Partial; existe evidencia parcial de escritura de cierre, sin GET integrado del escenario.

#### Traceability
FR-003, FR-006 | SC-003 | Evidencia parcial: src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#remarksArePassedAsCompletionNotesAndChangeMessage; src/test/java/com/seashare/m2/reservas/application/service/EndNavigationServiceTest.java#writesReservationThenCallsCu08ThenInsertsCheckout | In Process
