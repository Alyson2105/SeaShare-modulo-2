### Test Case Specification: TC-CU09-004 — Reintento y fallo seguro
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU09-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reintento y fallo seguro. Requisitos: FR-003, FR-008. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
timeoutRetriesOnceThenFailsSafe verifica timeout por separado; la integración con M1 real no se ejercita. El plan §4 menciona 500/502 y §5 enumera 503 y errores de acceso; el test vesselServerError500RegistersErrorRow verifica un solo intento para 500.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo HTTP; proveedor simulado responde 503 de forma persistente o conexión falla.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | FleetUnavailableTest#double503FailsSafeAfterSingleRetry (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y verifica dos llamadas HTTP para 503 persistente. timeoutRetriesOnceThenFailsSafe verifica timeout por separado; la integración con M1 real no se ejercita. El plan §4 menciona 500/502 y §5 enumera 503 y errores de acceso; el test vesselServerError500RegistersErrorRow verifica un solo intento para 500. |
| 2 | Ejecutar | Invocar la consulta y contar intentos. | Datos de las precondiciones | Máximo dos intentos; ante persistencia se lanza FLEET_SERVICE_UNAVAILABLE. | Pendiente | FleetUnavailableTest#double503FailsSafeAfterSingleRetry (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y verifica dos llamadas HTTP para 503 persistente. timeoutRetriesOnceThenFailsSafe verifica timeout por separado; la integración con M1 real no se ejercita. El plan §4 menciona 500/502 y §5 enumera 503 y errores de acceso; el test vesselServerError500RegistersErrorRow verifica un solo intento para 500. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | FleetUnavailableTest#double503FailsSafeAfterSingleRetry (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y verifica dos llamadas HTTP para 503 persistente. timeoutRetriesOnceThenFailsSafe verifica timeout por separado; la integración con M1 real no se ejercita. El plan §4 menciona 500/502 y §5 enumera 503 y errores de acceso; el test vesselServerError500RegistersErrorRow verifica un solo intento para 500. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El plan sección 4 menciona reintento para 500/502; sección 5 enumera 503 y errores de acceso. El escenario principal de 503 y timeout sigue siendo evaluable.

503 persistente se registra TIMEOUT; 500 y 502 no se reintentan en código, aunque el plan los menciona.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: FleetUnavailableTest#double503FailsSafeAfterSingleRetry. timeoutRetriesOnceThenFailsSafe verifica timeout por separado; la integración con M1 real no se ejercita. El plan §4 menciona 500/502 y §5 enumera 503 y errores de acceso; el test vesselServerError500RegistersErrorRow verifica un solo intento para 500.

#### Traceability
FR-003, FR-008 | SC-004 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java#double503FailsSafeAfterSingleRetry; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java#timeoutRetriesOnceThenFailsSafe; src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#vesselServerError500RegistersErrorRow | In Process
