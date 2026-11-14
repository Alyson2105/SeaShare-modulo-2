### Test Case Specification: TC-CU10-004 — Respuesta ilegible
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU10-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar respuesta ilegible. Requisitos: FR-002, FR-006. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Los métodos statusInvalidJsonThrowsAndRegistersSingleErrorRow y statusMissingFieldThrowsAndRegistersSingleErrorRow comprueban otras variantes; no prueban el bloqueo de reserva completo.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M1 simulado devuelve cuerpo vacío, JSON inválido o operational_status ausente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ExternalQueryLogRegistrationTest#statusEmptyBodyThrowsAndRegistersSingleErrorRow (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y fila ERROR con estado nulo. Los métodos statusInvalidJsonThrowsAndRegistersSingleErrorRow y statusMissingFieldThrowsAndRegistersSingleErrorRow comprueban otras variantes; no prueban el bloqueo de reserva completo. |
| 2 | Ejecutar | Consultar cada variante. | Datos de las precondiciones | El adaptador lanza FLEET_SERVICE_UNAVAILABLE y no considera disponible la embarcación. | Pendiente | ExternalQueryLogRegistrationTest#statusEmptyBodyThrowsAndRegistersSingleErrorRow (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y fila ERROR con estado nulo. Los métodos statusInvalidJsonThrowsAndRegistersSingleErrorRow y statusMissingFieldThrowsAndRegistersSingleErrorRow comprueban otras variantes; no prueban el bloqueo de reserva completo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ExternalQueryLogRegistrationTest#statusEmptyBodyThrowsAndRegistersSingleErrorRow (XML V4: passed). Aserta FLEET_SERVICE_UNAVAILABLE y fila ERROR con estado nulo. Los métodos statusInvalidJsonThrowsAndRegistersSingleErrorRow y statusMissingFieldThrowsAndRegistersSingleErrorRow comprueban otras variantes; no prueban el bloqueo de reserva completo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
No se convierte el error en false; se propaga excepción de dominio.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ExternalQueryLogRegistrationTest#statusEmptyBodyThrowsAndRegistersSingleErrorRow. Los métodos statusInvalidJsonThrowsAndRegistersSingleErrorRow y statusMissingFieldThrowsAndRegistersSingleErrorRow comprueban otras variantes; no prueban el bloqueo de reserva completo.

#### Traceability
FR-002, FR-006 | SC-003 | [fallos](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/FleetUnavailableTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#statusEmptyBodyThrowsAndRegistersSingleErrorRow; src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#statusInvalidJsonThrowsAndRegistersSingleErrorRow; src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#statusMissingFieldThrowsAndRegistersSingleErrorRow | In Process
