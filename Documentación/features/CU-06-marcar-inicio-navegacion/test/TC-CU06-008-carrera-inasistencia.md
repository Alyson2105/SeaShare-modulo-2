### Test Case Specification: TC-CU06-008 — Carrera con inasistencia
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-008

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar carrera con inasistencia. Requisitos: FR-001, FR-005, FR-009. Criterios: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Prueba no-show seguido de inicio secuencial; no prueba simultaneidad ni un único ganador. El endpoint y servicio existen. Las pruebas secuenciales no demuestran la carrera concurrente.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-06.

##### Preconditions & Default Setup
Reserva RESERVED después de la tolerancia; solicitudes concurrentes de inicio y no-show.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState: verde. Prueba no-show seguido de inicio secuencial; no prueba simultaneidad ni un único ganador. |
| 2 | Ejecutar | Coordinar ambas solicitudes y consultar estado y efectos. | Datos de las precondiciones | Solo una transición se confirma; si gana no-show, inicio es rechazado. | Pendiente | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState: verde. Prueba no-show seguido de inicio secuencial; no prueba simultaneidad ni un único ganador. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NoShowIntegrationTest#navigationStartAfterNoShowIsInvalidState: verde. Prueba no-show seguido de inicio secuencial; no prueba simultaneidad ni un único ganador. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Las pruebas secuenciales no demuestran la carrera concurrente.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001, FR-005, FR-009 | SC-001, SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#navigationStartAfterNoShowIsInvalidState | In Process
