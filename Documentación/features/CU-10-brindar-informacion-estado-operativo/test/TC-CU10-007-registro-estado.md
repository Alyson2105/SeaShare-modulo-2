### Test Case Specification: TC-CU10-007 — Registro de consulta
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU10-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar registro de consulta. Requisitos: FR-009. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-10 es integración saliente; M1 se simula y el modo fake es el predeterminado. Fallo de registro solo produce advertencia; no garantiza el 100 % de auditoría.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Modo HTTP y puerto de registro observable.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar un estado válido y verificar registro. | Datos de las precondiciones | Se intenta registrar OPERATIONAL_STATUS, identificador, resultado y estado crudo. | Pendiente | Integración: ExternalQueryLogRegistrationTest#statusAvailableRegistersOkWithRawStatus pasó: estado AVAILABLE, resultado OK y tipo OPERATIONAL_STATUS persistidos. No demuestra registro ante toda falla de almacenamiento. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Fallo de registro solo produce advertencia; no garantiza el 100 % de auditoría.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Evidencia parcial; consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-009 | SC-004 | [cliente](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ExternalQueryLogRegistrationTest.java#statusAvailableRegistersOkWithRawStatus | In Process
