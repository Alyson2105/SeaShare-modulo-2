### Test Case Specification: TC-CU04-002 — Clasificación por propietario
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar clasificación por propietario. Trazabilidad: FR-009, FR-010, FR-011; SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Con dependencias simuladas se comprueba la clasificación del propietario; faltan HTTP y persistencia real. La prueba ownerCanCancelAfterDeparture acepta una solicitud posterior al zarpe, mientras el spec limita este flujo al período previo.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED previa al zarpe, propietario registrado y causa logística válida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | RequestCancellationServiceTest#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable (XML V4: passed). Captura mechanicalFailure falso y aserta AVAILABLE, ENQUEUED y PUBLISHED sobre un resultado de CU-08 simulado; no comprueba entrega real. |
| 2 | Ejecutar | Solicitar cancelación como propietario. | Datos de las precondiciones | Según el plan, 200; CANCELLED y BY_OWNER sin clasificación por franja; motivo registrado. | Pendiente | RequestCancellationServiceTest#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable (XML V4: passed). Captura mechanicalFailure falso y aserta AVAILABLE, ENQUEUED y PUBLISHED sobre un resultado de CU-08 simulado; no comprueba entrega real. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | RequestCancellationServiceTest#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable (XML V4: passed). Captura mechanicalFailure falso y aserta AVAILABLE, ENQUEUED y PUBLISHED sobre un resultado de CU-08 simulado; no comprueba entrega real. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La captura visual del motivo es UI.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: RequestCancellationServiceTest#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable. Con dependencias simuladas se comprueba la clasificación del propietario; faltan HTTP y persistencia real. La prueba ownerCanCancelAfterDeparture acepta una solicitud posterior al zarpe, mientras el spec limita este flujo al período previo.

#### Traceability
FR-009, FR-010, FR-011 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#ownerWithLogisticsForceMajeureAndM1BlockedNotifiesAvailable; src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#ownerCanCancelAfterDeparture | In Process
