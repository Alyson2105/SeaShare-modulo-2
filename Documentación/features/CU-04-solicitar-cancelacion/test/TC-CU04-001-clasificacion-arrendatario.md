### Test Case Specification: TC-CU04-001 — Clasificación por anticipación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar clasificación por anticipación. Trazabilidad: FR-005, FR-006, FR-007, FR-008, FR-011; SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El clasificador cubre umbrales en pruebas unitarias, pero no hay evidencia HTTP y persistida para las cinco solicitudes; FR-005 exige CU-09 y el servicio usa la zona guardada.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED de arrendatario titular, hora de zarpe conocida y zona horaria oficial del puerto. Reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | CancellationClassifierTest#exactly72HoursIsFlexible (XML V4: passed). Comprueba que exactamente 72 horas produce FLEXIBLE; no demuestra la transición ni persistencia de las cinco reservas. |
| 2 | Ejecutar | Enviar solicitudes independientes con anticipación de 72 h exactas, un segundo antes de 72 h, 24 h exactas, un segundo antes de 24 h y un minuto antes del zarpe. | Datos de las precondiciones | Clasificaciones respectivas FLEXIBLE, MODERATE, MODERATE, LATE y LATE; cada reserva cambia a CANCELLED. | Pendiente | CancellationClassifierTest#exactly72HoursIsFlexible (XML V4: passed). Comprueba que exactamente 72 horas produce FLEXIBLE; no demuestra la transición ni persistencia de las cinco reservas. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | CancellationClassifierTest#exactly72HoursIsFlexible (XML V4: passed). Comprueba que exactamente 72 horas produce FLEXIBLE; no demuestra la transición ni persistencia de las cinco reservas. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
El plan especifica truncar a minutos antes de comparar; CancellationClassifierTest prueba los límites de 72 h y 24 h en unidad.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: CancellationClassifierTest#exactly72HoursIsFlexible. El clasificador cubre umbrales en pruebas unitarias, pero no hay evidencia HTTP y persistida para las cinco solicitudes; FR-005 exige CU-09 y el servicio usa la zona guardada.

#### Traceability
FR-005, FR-006, FR-007, FR-008, FR-011 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/domain/service/CancellationClassifierTest.java#exactly72HoursIsFlexible; src/test/java/com/seashare/m2/reservas/domain/service/CancellationClassifierTest.java#oneSecondLessThan72HoursIsModerate; src/test/java/com/seashare/m2/reservas/domain/service/CancellationClassifierTest.java#exactly24HoursIsModerate; src/test/java/com/seashare/m2/reservas/domain/service/CancellationClassifierTest.java#oneSecondLessThan24HoursIsLate; src/test/java/com/seashare/m2/reservas/domain/service/CancellationClassifierTest.java#oneMinuteBeforeIsLateWithOneMinute | In Process
