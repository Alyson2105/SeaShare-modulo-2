### Test Case Specification: TC-CU10-002 — Estados no disponibles
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU10-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar estados no disponibles. Requisitos: FR-003, FR-004, FR-005. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CreateReservationValidationTest#vesselNotAvailableIs409 prueba rechazo con puerto simulado; el puerto booleano no comunica motivo específico de FR-005.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M1 simulado devuelve por separado RESERVED, IN_NAVIGATION y MAINTENANCE_CLEANING.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpFleetClientTest#nonAvailableStatusesAreFalse (XML V4: passed). Aserta false para RESERVED, IN_NAVIGATION y MAINTENANCE_CLEANING mediante WireMock. CreateReservationValidationTest#vesselNotAvailableIs409 prueba rechazo con puerto simulado; el puerto booleano no comunica motivo específico de FR-005. |
| 2 | Ejecutar | Consultar cada estado mediante el puerto. | Datos de las precondiciones | Cada resultado es false; CU-02 impide crear reserva cuando recibe false. | Pendiente | HttpFleetClientTest#nonAvailableStatusesAreFalse (XML V4: passed). Aserta false para RESERVED, IN_NAVIGATION y MAINTENANCE_CLEANING mediante WireMock. CreateReservationValidationTest#vesselNotAvailableIs409 prueba rechazo con puerto simulado; el puerto booleano no comunica motivo específico de FR-005. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpFleetClientTest#nonAvailableStatusesAreFalse (XML V4: passed). Aserta false para RESERVED, IN_NAVIGATION y MAINTENANCE_CLEANING mediante WireMock. CreateReservationValidationTest#vesselNotAvailableIs409 prueba rechazo con puerto simulado; el puerto booleano no comunica motivo específico de FR-005. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El puerto booleano no comunica al consumidor el motivo específico requerido por FR-005.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: HttpFleetClientTest#nonAvailableStatusesAreFalse. CreateReservationValidationTest#vesselNotAvailableIs409 prueba rechazo con puerto simulado; el puerto booleano no comunica motivo específico de FR-005.

#### Traceability
FR-003, FR-004, FR-005 | SC-002 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java#nonAvailableStatusesAreFalse; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/CreateReservationValidationTest.java#vesselNotAvailableIs409 | In Process
