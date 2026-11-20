### Test Case Specification: TC-CU15-002 — Consulta de reserva pendiente
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU15-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consulta de reserva pendiente. Requisitos: FR-002, FR-005, FR-007. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Spec Edge Cases «Naturaleza Estrictamente Idempotente» pide exactamente el mismo resultado en consultas sucesivas; plan.md Responsabilidades §2 exige remaining_seconds variable.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas; las pruebas citadas cubren solo parte del escenario.

##### Preconditions & Default Setup
Reserva PENDING_PAYMENT con ttl_expires_at conocido y reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | GetInternalReservationServiceTest#ttlAppliesForPendingPayment (XML V4: passed). Aserta ttlApplies true y 120 segundos restantes con reloj y repositorio simulados. Spec Edge Cases «Naturaleza Estrictamente Idempotente» pide exactamente el mismo resultado en consultas sucesivas; plan.md Responsabilidades §2 exige remaining_seconds variable. |
| 2 | Ejecutar | Consultar la reserva. | Datos de las precondiciones | El plan prevé expiración y segundos restantes coherentes con el reloj, sin reiniciar TTL. | Pendiente | GetInternalReservationServiceTest#ttlAppliesForPendingPayment (XML V4: passed). Aserta ttlApplies true y 120 segundos restantes con reloj y repositorio simulados. Spec Edge Cases «Naturaleza Estrictamente Idempotente» pide exactamente el mismo resultado en consultas sucesivas; plan.md Responsabilidades §2 exige remaining_seconds variable. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | GetInternalReservationServiceTest#ttlAppliesForPendingPayment (XML V4: passed). Aserta ttlApplies true y 120 segundos restantes con reloj y repositorio simulados. Spec Edge Cases «Naturaleza Estrictamente Idempotente» pide exactamente el mismo resultado en consultas sucesivas; plan.md Responsabilidades §2 exige remaining_seconds variable. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Consultas repetidas pueden devolver segundos distintos sin mutar la reserva.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: GetInternalReservationServiceTest#ttlAppliesForPendingPayment. Spec Edge Cases «Naturaleza Estrictamente Idempotente» pide exactamente el mismo resultado en consultas sucesivas; plan.md Responsabilidades §2 exige remaining_seconds variable.

#### Traceability
FR-002, FR-005, FR-007 | SC-002 | Prueba parcial ejecutada | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/GetInternalReservationServiceTest.java#ttlAppliesForPendingPayment | Draft

#### Decisión aprobada — 2026-11-13
Se aprobó el GET sin efectos con remaining_seconds variable según el reloj. Esta decisión no modifica automáticamente el veredicto del TC.
