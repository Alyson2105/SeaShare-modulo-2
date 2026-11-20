### Test Case Specification: TC-CU10-005 — Reintento y fallo seguro
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU10-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reintento y fallo seguro. Requisitos: FR-002, FR-006. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No cuenta intentos ni verifica 503 persistente y bloqueo efectivo del consumidor.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M1 simulado responde 503 persistente o falla la conexión.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ExternalQueryLogRegistrationTest#statusTimeoutRegistersTimeoutRow (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y fila TIMEOUT para respuesta WireMock demorada. No cuenta intentos ni verifica 503 persistente y bloqueo efectivo del consumidor. |
| 2 | Ejecutar | Invocar consulta y contar intentos. | Datos de las precondiciones | Máximo dos intentos; agotados, FLEET_SERVICE_UNAVAILABLE impide continuar. | Pendiente | ExternalQueryLogRegistrationTest#statusTimeoutRegistersTimeoutRow (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y fila TIMEOUT para respuesta WireMock demorada. No cuenta intentos ni verifica 503 persistente y bloqueo efectivo del consumidor. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ExternalQueryLogRegistrationTest#statusTimeoutRegistersTimeoutRow (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y fila TIMEOUT para respuesta WireMock demorada. No cuenta intentos ni verifica 503 persistente y bloqueo efectivo del consumidor. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Código reintenta 503 y errores de acceso; 500 y 502 no se reintentan pese a la tabla del plan.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ExternalQueryLogRegistrationTest#statusTimeoutRegistersTimeoutRow. No cuenta intentos ni verifica 503 persistente y bloqueo efectivo del consumidor.

#### Traceability
FR-002, FR-006 | SC-003 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#statusTimeoutRegistersTimeoutRow | In Process
