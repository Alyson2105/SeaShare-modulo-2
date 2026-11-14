### Test Case Specification: TC-CU05-002 — Rechazo antes de la tolerancia
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo antes de la tolerancia. Requisitos: FR-003, FR-004, FR-005. Criterios: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El límite de 29:59 se verifica mediante HTTP y PostgreSQL; 18 minutos se verifica con mocks. El plan requiere campos separados de tiempo restante y no se inspecciona outbox.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-05.

##### Preconditions & Default Setup
Reserva RESERVED y reloj controlado; zarpe ya ocurrió.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NoShowIntegrationTest#oneSecondBeforeGraceEndIsRejected (XML V4: passed). Aserta 409 GRACE_PERIOD_NOT_ELAPSED, reserva RESERVED y cero filas noshow_event; no verifica los campos de tiempo restante. |
| 2 | Ejecutar | Intentar reportar a los 29 minutos y 59 segundos y en otra solicitud a los 18 minutos. | Datos de las precondiciones | Según el plan, 409 GRACE_PERIOD_NOT_ELAPSED con tiempo restante exacto; reserva y notificaciones sin cambios. | Pendiente | NoShowIntegrationTest#oneSecondBeforeGraceEndIsRejected (XML V4: passed). Aserta 409 GRACE_PERIOD_NOT_ELAPSED, reserva RESERVED y cero filas noshow_event; no verifica los campos de tiempo restante. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NoShowIntegrationTest#oneSecondBeforeGraceEndIsRejected (XML V4: passed). Aserta 409 GRACE_PERIOD_NOT_ELAPSED, reserva RESERVED y cero filas noshow_event; no verifica los campos de tiempo restante. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Antes de la hora de zarpe el plan prevé 409 TRIP_NOT_STARTED.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NoShowIntegrationTest#oneSecondBeforeGraceEndIsRejected. El límite de 29:59 se verifica mediante HTTP y PostgreSQL; 18 minutos se verifica con mocks. El plan requiere campos separados de tiempo restante y no se inspecciona outbox.

#### Traceability
FR-003, FR-004, FR-005 | SC-001, SC-003 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#oneSecondBeforeGraceEndIsRejected; src/test/java/com/seashare/m2/reservas/application/service/ReportNoShowServiceTest.java#eighteenMinutesAfterDepartureShowsTwelveMinutesAndZeroSeconds | In Process
