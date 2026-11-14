### Test Case Specification: TC-CU10-006 — Embarcación inexistente
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU10-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar embarcación inexistente. Requisitos: FR-001, FR-002, FR-006. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
SC-005 exige mensaje claro y continuidad del sistema; no se demuestra respuesta de los consumidores.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M1 simulado responde 404.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | FleetUnavailableTest#operationalStatusNotFoundOn404 (XML V4: passed). Aserta VESSEL_NOT_FOUND y verifica una llamada HTTP con WireMock. SC-005 exige mensaje claro y continuidad del sistema; no se demuestra respuesta de los consumidores. |
| 2 | Ejecutar | Consultar identificador inexistente. | Datos de las precondiciones | El adaptador lanza VESSEL_NOT_FOUND sin reintento. | Pendiente | FleetUnavailableTest#operationalStatusNotFoundOn404 (XML V4: passed). Aserta VESSEL_NOT_FOUND y verifica una llamada HTTP con WireMock. SC-005 exige mensaje claro y continuidad del sistema; no se demuestra respuesta de los consumidores. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | FleetUnavailableTest#operationalStatusNotFoundOn404 (XML V4: passed). Aserta VESSEL_NOT_FOUND y verifica una llamada HTTP con WireMock. SC-005 exige mensaje claro y continuidad del sistema; no se demuestra respuesta de los consumidores. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El error de dominio no es por sí solo una respuesta HTTP.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: FleetUnavailableTest#operationalStatusNotFoundOn404. SC-005 exige mensaje claro y continuidad del sistema; no se demuestra respuesta de los consumidores.

#### Traceability
FR-001, FR-002, FR-006 | SC-005 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java#operationalStatusNotFoundOn404 | In Process
