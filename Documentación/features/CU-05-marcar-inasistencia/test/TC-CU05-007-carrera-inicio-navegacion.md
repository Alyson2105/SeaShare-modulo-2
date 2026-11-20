### Test Case Specification: TC-CU05-007 — Carrera con inicio de viaje
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar carrera con inicio de viaje. Requisitos: FR-001, FR-006, FR-007. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La prueba ejecuta inasistencia seguida de inicio, no dos operaciones concurrentes ni la variante en que el inicio gana la carrera.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-05.

##### Preconditions & Default Setup
Reserva RESERVED tras tolerancia; solicitudes concurrentes de inasistencia e inicio de navegación.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState (XML V4: passed). Aserta primer HTTP 200 y posterior 409 INVALID_RESERVATION_STATE; no ejecuta transacciones simultáneas. |
| 2 | Ejecutar | Coordinar ambas operaciones y revisar estado final y efectos. | Datos de las precondiciones | Solo una transición prevalece; si navegación se confirma primero, el reporte es rechazado. | Pendiente | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState (XML V4: passed). Aserta primer HTTP 200 y posterior 409 INVALID_RESERVATION_STATE; no ejecuta transacciones simultáneas. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState (XML V4: passed). Aserta primer HTTP 200 y posterior 409 INVALID_RESERVATION_STATE; no ejecuta transacciones simultáneas. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-05 y CU-06 tienen endpoints; la prueba secuencial no demuestra esta carrera completa.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState. La prueba ejecuta inasistencia seguida de inicio, no dos operaciones concurrentes ni la variante en que el inicio gana la carrera.

#### Traceability
FR-001, FR-006, FR-007 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#navigationStartAfterNoShowIsInvalidState | In Process
