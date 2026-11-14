### Test Case Specification: TC-CU05-001 — Reporte al cumplir la tolerancia
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reporte al cumplir la tolerancia. Requisitos: FR-001, FR-003, FR-004, FR-007, FR-008. Criterios: SC-001, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El HTTP integrado comprueba el límite de 30 minutos y efectos persistidos. No demuestra la consulta a CU-09 ni el primer intento externo de SC-004 en menos de un segundo; 35 minutos se prueban solo con mocks.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-05.

##### Preconditions & Default Setup
Reserva RESERVED del propietario, con zarpe conocido y reloj controlado en zona del puerto.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (XML V4: passed). Aserta HTTP 200, CANCELLED, NO_SHOW, 30 minutos y AVAILABLE; consulta en PostgreSQL estado, subtipo y una fila con propietario, espera y observaciones. |
| 2 | Ejecutar | Reportar la inasistencia exactamente 30 minutos después del zarpe. | Datos de las precondiciones | Según el plan, 200; CANCELLED, subtipo NO_SHOW y 30 minutos registrados; se invoca CU-08. | Pasó | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (XML V4: passed). Aserta HTTP 200, CANCELLED, NO_SHOW, 30 minutos y AVAILABLE; consulta en PostgreSQL estado, subtipo y una fila con propietario, espera y observaciones. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pasó | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (XML V4: passed). Aserta HTTP 200, CANCELLED, NO_SHOW, 30 minutos y AVAILABLE; consulta en PostgreSQL estado, subtipo y una fila con propietario, espera y observaciones. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El caso de 35 minutos y 40 segundos se comprueba en ReportNoShowServiceTest#thirtyFiveMinutesFortySecondsRecords35Minutes con dependencias simuladas; no demuestra integración completa.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation. El HTTP integrado comprueba el límite de 30 minutos y efectos persistidos. No demuestra la consulta a CU-09 ni el primer intento externo de SC-004 en menos de un segundo; 35 minutos se prueban solo con mocks.

#### Traceability
FR-001, FR-003, FR-004, FR-007, FR-008 | SC-001, SC-004 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#successfulNoShowPersistsEventAndCancelsReservation; src/test/java/com/seashare/m2/reservas/application/service/ReportNoShowServiceTest.java#thirtyFiveMinutesFortySecondsRecords35Minutes | In Process
