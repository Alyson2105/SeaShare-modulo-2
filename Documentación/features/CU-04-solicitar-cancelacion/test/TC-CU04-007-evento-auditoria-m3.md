### Test Case Specification: TC-CU04-007 — Auditoría y notificación M3
**Status**: Blocked
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
CU-04 y el endpoint propuesto aún no están implementados. Los resultados son contractuales, no observados. El contrato final de payload M3 T-12 está pendiente; PUBLISHED en plan no demuestra confirmación de consumo.

##### Dependencies & Assumptions
Ejecución futura con CU-04 implementado y dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); no demuestran la orquestación completa.

##### Preconditions & Default Setup
Reserva RESERVED válida; observar auditoría y eventos relacionados con la reserva.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Solicitar cancelación y consultar auditoría y outbox. | Datos de las precondiciones | Se registra actor, fecha, clasificación y datos aplicables; se comunica a M3 el estado y subtipo sin cálculos monetarios en M2. | Pendiente | ChangeReservationStatusServiceTest#cancelledByRenterStoresSubtypeAndAnticipation pasó: integración del motor verifica subtipo y actor en evento simulado; falta flujo de cancelación y auditoría completa. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin publicación real |

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
Ejecutado el 2026-11-13: Blocked; la evidencia solo cubre el componente compartido, el flujo está bloqueado.

#### Traceability
FR-013, FR-014, FR-015 | SC-006, SC-007 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#cancelledByRenterStoresSubtypeAndAnticipation | Blocked
