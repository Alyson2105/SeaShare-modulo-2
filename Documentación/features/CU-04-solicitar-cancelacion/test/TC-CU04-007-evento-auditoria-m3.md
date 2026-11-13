### Test Case Specification: TC-CU04-007 — Auditoría y notificación M3
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar auditoría y notificación m3. Trazabilidad: FR-013, FR-014, FR-015; SC-006, SC-007.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
PUBLISHED se deriva del destino encolado en RequestCancellationService#execute; no demuestra confirmación de M3. El payload completo y la ausencia integral de cálculos requieren evidencia.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED válida; observar auditoría y eventos relacionados con la reserva.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | RequestCancellationServiceTest#renterWith72HoursCancelsFlexible: verde con puertos simulados; no acredita persistencia real ni entrega a M3. |
| 2 | Ejecutar | Solicitar cancelación y consultar auditoría y outbox. | Datos de las precondiciones | Se registra actor, fecha, clasificación y datos aplicables; se comunica a M3 el estado y subtipo sin cálculos monetarios en M2. | Pendiente | RequestCancellationServiceTest#renterWith72HoursCancelsFlexible: verde con puertos simulados; no acredita persistencia real ni entrega a M3. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | RequestCancellationServiceTest#renterWith72HoursCancelsFlexible: verde con puertos simulados; no acredita persistencia real ni entrega a M3. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
El contrato final de payload M3 T-12 está pendiente; PUBLISHED en plan no demuestra confirmación de consumo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-013, FR-014, FR-015 | SC-006, SC-007 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#renterWith72HoursCancelsFlexible | In Process
