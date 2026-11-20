### Test Case Specification: TC-CU08-003 — Conflicto de versión
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar conflicto de versión. Requisitos: FR-004, FR-006. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No compara el resto de campos ni los eventos tras el rechazo.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva existente con versión conocida y comando con expected_version diferente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ChangeReservationStatusServiceTest#expectedVersionMismatchIsRejected (XML V4: passed). Aserta VersionConflictException con VERSION_CONFLICT y estado persistido INITIATED. No compara el resto de campos ni los eventos tras el rechazo. |
| 2 | Ejecutar | Invocar transición interna válida con versión obsoleta. | Datos de las precondiciones | El servicio rechaza con VersionConflictException sin sobrescribir la reserva. | Pendiente | ChangeReservationStatusServiceTest#expectedVersionMismatchIsRejected (XML V4: passed). Aserta VersionConflictException con VERSION_CONFLICT y estado persistido INITIATED. No compara el resto de campos ni los eventos tras el rechazo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ChangeReservationStatusServiceTest#expectedVersionMismatchIsRejected (XML V4: passed). Aserta VersionConflictException con VERSION_CONFLICT y estado persistido INITIATED. No compara el resto de campos ni los eventos tras el rechazo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El motor interno no es endpoint HTTP; 409 es contrato de presentación, no resultado directo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ChangeReservationStatusServiceTest#expectedVersionMismatchIsRejected. No compara el resto de campos ni los eventos tras el rechazo.

#### Traceability
FR-004, FR-006 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#versionConflictRaisesVERSION_CONFLICT, src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#expectedVersionMismatchIsRejected | In Process
