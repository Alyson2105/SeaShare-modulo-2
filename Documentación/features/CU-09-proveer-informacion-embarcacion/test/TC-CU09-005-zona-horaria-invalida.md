### Test Case Specification: TC-CU09-005 — Zona horaria ausente o inválida
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU09-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar zona horaria ausente o inválida. Requisitos: FR-004, FR-008. Criterios: SC-004, SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
invalidHarborTimezoneFailsSafe prueba zona inválida por separado; no se demuestran todos los consumidores ni la integración M1 real.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo HTTP y respuesta M1 con owner y capacidad válidos pero timezone ausente o inválida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | FleetUnavailableTest#missingHarborTimezoneFailsSafe (XML V4: passed). Aserta HARBOR_TIMEZONE_UNAVAILABLE para zona ausente con WireMock. invalidHarborTimezoneFailsSafe prueba zona inválida por separado; no se demuestran todos los consumidores ni la integración M1 real. |
| 2 | Ejecutar | Invocar consulta con cada respuesta. | Datos de las precondiciones | El adaptador rechaza con HARBOR_TIMEZONE_UNAVAILABLE, sin asumir zona predeterminada. | Pendiente | FleetUnavailableTest#missingHarborTimezoneFailsSafe (XML V4: passed). Aserta HARBOR_TIMEZONE_UNAVAILABLE para zona ausente con WireMock. invalidHarborTimezoneFailsSafe prueba zona inválida por separado; no se demuestran todos los consumidores ni la integración M1 real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | FleetUnavailableTest#missingHarborTimezoneFailsSafe (XML V4: passed). Aserta HARBOR_TIMEZONE_UNAVAILABLE para zona ausente con WireMock. invalidHarborTimezoneFailsSafe prueba zona inválida por separado; no se demuestran todos los consumidores ni la integración M1 real. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El contrato de timezone aún debe acordarse con M1.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: FleetUnavailableTest#missingHarborTimezoneFailsSafe. invalidHarborTimezoneFailsSafe prueba zona inválida por separado; no se demuestran todos los consumidores ni la integración M1 real.

#### Traceability
FR-004, FR-008 | SC-004, SC-005 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java#missingHarborTimezoneFailsSafe; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java#invalidHarborTimezoneFailsSafe | In Process
