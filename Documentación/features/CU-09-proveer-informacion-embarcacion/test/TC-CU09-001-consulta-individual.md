### Test Case Specification: TC-CU09-001 — Consulta de datos de embarcación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU09-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consulta de datos de embarcación. Requisitos: FR-001, FR-003, FR-004. Criterios: SC-003, SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
FleetInfoPort devuelve DTO reducido; no demuestra ficha completa, GPS ni integración con M1 real.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo seashare.fleet.mode=http; M1 simulado devuelve owner.owner_id UUID, max_capacity y port.timezone IANA válidos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpFleetClientTest#mapsVesselInfoFromSuccessfulResponse (XML V4: passed). Aserta ownerId, maxCapacity y harborTimezone usando respuesta WireMock. FleetInfoPort devuelve DTO reducido; no demuestra ficha completa, GPS ni integración con M1 real. |
| 2 | Ejecutar | Invocar FleetInfoPort.getVesselInfo con UUID de embarcación. | Datos de las precondiciones | El adaptador solicita GET /api/v1/vessels/{vessel_id} y devuelve ownerId, maxCapacity y harborTimezone. | Pendiente | HttpFleetClientTest#mapsVesselInfoFromSuccessfulResponse (XML V4: passed). Aserta ownerId, maxCapacity y harborTimezone usando respuesta WireMock. FleetInfoPort devuelve DTO reducido; no demuestra ficha completa, GPS ni integración con M1 real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpFleetClientTest#mapsVesselInfoFromSuccessfulResponse (XML V4: passed). Aserta ownerId, maxCapacity y harborTimezone usando respuesta WireMock. FleetInfoPort devuelve DTO reducido; no demuestra ficha completa, GPS ni integración con M1 real. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
FleetInfoPort devuelve un DTO reducido; hay otro adaptador de detalle con GPS. El contrato M1 y zona horaria no están firmados.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpFleetClientTest#mapsVesselInfoFromSuccessfulResponse. FleetInfoPort devuelve DTO reducido; no demuestra ficha completa, GPS ni integración con M1 real.

#### Traceability
FR-001, FR-003, FR-004 | SC-003, SC-005 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java#mapsVesselInfoFromSuccessfulResponse; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java#sendsInternalServiceTokenHeader | In Process
