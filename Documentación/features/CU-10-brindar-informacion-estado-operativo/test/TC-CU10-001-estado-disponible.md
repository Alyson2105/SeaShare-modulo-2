### Test Case Specification: TC-CU10-001 — Estado disponible
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU10-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estado disponible. Requisitos: FR-001, FR-002, FR-003, FR-004. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No comprueba M1 real ni demuestra SC-002 para estados no disponibles.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo HTTP; M1 simulado responde operational_status AVAILABLE.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpFleetClientTest#availableStatusIsTrue (XML V4: passed). Aserta true para AVAILABLE y verifica ruta y token de servicio con WireMock. No comprueba M1 real ni demuestra SC-002 para estados no disponibles. |
| 2 | Ejecutar | Invocar FleetOperationalStatusPort.isAvailable con UUID. | Datos de las precondiciones | El adaptador consulta /api/v1/vessels/{vessel_id}/operational-status y devuelve true. | Pendiente | HttpFleetClientTest#availableStatusIsTrue (XML V4: passed). Aserta true para AVAILABLE y verifica ruta y token de servicio con WireMock. No comprueba M1 real ni demuestra SC-002 para estados no disponibles. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpFleetClientTest#availableStatusIsTrue (XML V4: passed). Aserta true para AVAILABLE y verifica ruta y token de servicio con WireMock. No comprueba M1 real ni demuestra SC-002 para estados no disponibles. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La consulta es puntual y no establece exclusividad FCFS.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpFleetClientTest#availableStatusIsTrue. No comprueba M1 real ni demuestra SC-002 para estados no disponibles.

#### Traceability
FR-001, FR-002, FR-003, FR-004 | SC-002 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java#availableStatusIsTrue | In Process
