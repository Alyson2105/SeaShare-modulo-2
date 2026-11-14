### Test Case Specification: TC-CU15-004 — Consulta de reserva cancelada
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU15-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consulta de reserva cancelada. Requisitos: FR-004, FR-006. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-15: la lectura local devuelve minutos persistidos del arrendatario y conserva nulo para propietario; falta verificar el flujo HTTP completo y la autenticación de servicio.

##### Dependencies & Assumptions
La lectura integrada prueba subtipo, actor y anticipación persistida; no acredita autenticación real de M3.

##### Preconditions & Default Setup
Reserva CANCELLED con subtipo, actor y anticipación registrados.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor (XML V4: passed). Aserta CANCELLED, BY_OWNER y actor OWNER desde auditoría persistida. InternalReservationResponse#from devuelve null para cancellation_anticipation_hours; el dato obligatorio de FR-004 no se expone. |
| 2 | Ejecutar | Consultar la reserva como M3. | Datos de las precondiciones | El plan prevé subtipo, actor y anticipación sin cálculo de reembolso. | Pendiente | InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor (XML V4: passed). Aserta CANCELLED, BY_OWNER y actor OWNER desde auditoría persistida. InternalReservationResponse#from devuelve null para cancellation_anticipation_hours; el dato obligatorio de FR-004 no se expone. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor (XML V4: passed). Aserta CANCELLED, BY_OWNER y actor OWNER desde auditoría persistida. InternalReservationResponse#from devuelve null para cancellation_anticipation_hours; el dato obligatorio de FR-004 no se expone. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La anticipación es nula cuando no aplica, por ejemplo para cancelación del propietario; para el arrendatario se deriva de minutos persistidos.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Blocked; estado documental: Blocked; evidencia: InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor. InternalReservationResponse#from devuelve null para cancellation_anticipation_hours; el dato obligatorio de FR-004 no se expone.

#### Traceability
FR-004, FR-006 | SC-003 | Prueba parcial ejecutada | Evidencia: src/test/java/com/seashare/m2/reservas/integration/InternalReservationReadIntegrationTest.java#cancelledByOwnerIsReportedWithOwnerActor | Blocked

#### Evidencia local posterior a V4 — 2026-11-13
InternalReservationReadIntegrationTest#cancelledByRenterReportsPersistedAnticipation y #cancelledByOwnerIsReportedWithOwnerActor; PostgreSQL local; corrida focalizada 2026-11-13, 18 pruebas sin fallos. Veredicto técnico: Partial. No acredita integración real M1/M3 ni resuelve las decisiones contractuales pendientes.
