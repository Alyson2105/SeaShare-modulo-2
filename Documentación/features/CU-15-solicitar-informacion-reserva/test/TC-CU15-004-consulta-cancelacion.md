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
CU-15 expone un endpoint interno con identidad temporal por headers; JWT de servicio está pendiente. CU-04 está implementado; InternalReservationResponse#from devuelve null para anticipación.

##### Dependencies & Assumptions
La lectura integrada prueba subtipo y actor; no acredita anticipación.

##### Preconditions & Default Setup
Reserva CANCELLED con subtipo, actor y anticipación registrados.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar la reserva como M3. | Datos de las precondiciones | El plan prevé subtipo, actor y anticipación sin cálculo de reembolso. | Pendiente | Integración: InternalReservationReadIntegrationTest#cancelledByOwnerIsReportedWithOwnerActor pasó para subtipo y actor desde auditoría; InternalReservationResponse#from devuelve null para anticipación pese a que CU-04 está implementado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-04 está implementado; InternalReservationResponse#from devuelve null para anticipación.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Blocked; evidencia parcial, consultar el flujo.

#### Traceability
FR-004, FR-006 | SC-003 | Prueba parcial ejecutada | Evidencia: src/test/java/com/seashare/m2/reservas/integration/InternalReservationReadIntegrationTest.java#cancelledByOwnerIsReportedWithOwnerActor | Blocked
