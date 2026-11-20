### Test Case Specification: TC-CU05-006 — Registro y notificación de inasistencia
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar registro y notificación de inasistencia. Requisitos: FR-007, FR-008, FR-009, FR-010, FR-011. Criterios: SC-004, SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Se demuestra transición y registro persistidos al minuto 30; faltan la inspección del outbox, el primer intento en menos de un segundo y la entrega externa.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); no demuestran el flujo completo de CU-05.

##### Preconditions & Default Setup
Reserva RESERVED válida con guarda de bloqueo M1; esperar al menos 30 minutos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (XML V4: passed). Aserta CANCELLED y NO_SHOW en respuesta y reserva, una fila de evento con propietario, espera y observaciones; no consulta outbox ni entregas. |
| 2 | Ejecutar | Confirmar reporte y revisar registro, outbox y notificaciones de prueba. | Datos de las precondiciones | Registrar propietario, hora, minutos y observaciones; CANCELLED/NO_SHOW; solicitar AVAILABLE a M1 y comunicar subtipo a M3 sin calcular dinero. | Pendiente | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (XML V4: passed). Aserta CANCELLED y NO_SHOW en respuesta y reserva, una fila de evento con propietario, espera y observaciones; no consulta outbox ni entregas. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation (XML V4: passed). Aserta CANCELLED y NO_SHOW en respuesta y reserva, una fila de evento con propietario, espera y observaciones; no consulta outbox ni entregas. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Encolar no prueba entrega ni el primer intento en menos de un segundo; payload M3 T-12 pendiente.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NoShowIntegrationTest#successfulNoShowPersistsEventAndCancelsReservation. Se demuestra transición y registro persistidos al minuto 30; faltan la inspección del outbox, el primer intento en menos de un segundo y la entrega externa.

#### Traceability
FR-007, FR-008, FR-009, FR-010, FR-011 | SC-004, SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#successfulNoShowPersistsEventAndCancelsReservation | In Process
