### Test Case Specification: TC-CU06-006 — Notificaciones y registro
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar notificaciones y registro. Requisitos: FR-005, FR-006, FR-007, FR-008, FR-010. Criterios: SC-002, SC-004, SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-06 y su endpoint aún no existen; los resultados son expectativas del contrato. Una fila outbox no demuestra entrega a M1, activación del seguro ni primer intento en menos de un segundo; insurance_activated del plan requiere confirmación externa.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); no demuestran el flujo completo de CU-06.

##### Preconditions & Default Setup
Reserva RESERVED del propietario, fecha válida y dependencias de prueba controladas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Iniciar navegación y consultar registro, auditoría y mensajes relacionados. | Datos de las precondiciones | Registrar hora real y notas; solicitar IN_NAVIGATION a M1 y notificar M3 sin cálculos ni cobros en M2. | Pendiente | ModuleNotificationMatrixTest#inNavigationAndCompletedNotifyModuleOne pasó: matriz unitaria decide IN_NAVIGATION para M1; faltan endpoint, registro y entrega; ChangeReservationStatusServiceTest#checkInAndCheckOutTransitionsWork pasó: motor encola AMQP y M1_HTTP simulados. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Una fila outbox no demuestra entrega a M1, activación del seguro ni primer intento en menos de un segundo; insurance_activated del plan requiere confirmación externa.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Blocked; la evidencia solo cubre el componente compartido, el flujo está bloqueado.

#### Traceability
FR-005, FR-006, FR-007, FR-008, FR-010 | SC-002, SC-004, SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#inNavigationAndCompletedNotifyModuleOne; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#checkInAndCheckOutTransitionsWork | Blocked
