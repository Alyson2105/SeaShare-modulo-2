### Test Case Specification: TC-CU09-006 — Registro de consulta
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU09-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar registro de consulta. Requisitos: FR-010. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-09 es integración saliente; M1 se simula y el modo fake es el predeterminado. REQUIRES_NEW puede perder el registro si no hay conexión disponible; no demuestra 100 % de trazabilidad.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo HTTP y puerto de registro observable.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar M1 y observar el registro final de una consulta. | Datos de las precondiciones | Se intenta registrar una entrada VESSEL_INFO con resultado final; fallo de registro no cambia el resultado de consulta. | Pendiente | Integración: ExternalQueryLogRegistrationTest#vesselInfoOkRegistersOkRow y #registrationFailureDoesNotBreakQuery pasaron: fila OK y consulta exitosa con registro fallido. No garantiza trazabilidad ante fallos del almacén. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
REQUIRES_NEW puede perder el registro si no hay conexión disponible; no demuestra 100 % de trazabilidad.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial. Evidencia parcial; consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-010 | SC-003 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#vesselInfoOkRegistersOkRow; src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#registrationFailureDoesNotBreakQuery | In Process
