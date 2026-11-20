### Test Case Specification: TC-CU04-008 — Carrera de cancelación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-008

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar carrera de cancelación. Trazabilidad: FR-002, FR-011; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El camino de cancelación existe, pero no se identificó prueba V4 que ejecute la carrera de dos solicitudes de cancelación con sus efectos persistidos.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED y solicitudes simultáneas de titular y propietario sobre la misma versión.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente de la carrera entre arrendatario y propietario. |
| 2 | Ejecutar | Coordinar ambas solicitudes y observar transiciones y respuesta perdedora. | Datos de las precondiciones | Solo una transición confirmada; el plan contempla 409 CONCURRENT_STATE_CHANGE para la perdedora. | Pendiente | Sin ejecución automatizada pertinente de la carrera entre arrendatario y propietario. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente de la carrera entre arrendatario y propietario. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
También verificar competencia con inicio de navegación; el motor usa control de versión, no equivale al endpoint.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Pendiente; sin ejecución automatizada pertinente. El camino de cancelación existe, pero no se identificó prueba V4 que ejecute la carrera de dos solicitudes de cancelación con sus efectos persistidos.

#### Traceability
FR-002, FR-011 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Sin prueba automatizada pertinente | In Process
