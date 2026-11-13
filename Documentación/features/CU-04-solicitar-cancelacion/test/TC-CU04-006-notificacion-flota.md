### Test Case Specification: TC-CU04-006 — Destino operativo M1
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar destino operativo m1. Trazabilidad: FR-010, FR-011, FR-012; SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-04 y el endpoint propuesto aún no están implementados. Los resultados son contractuales, no observados. El primer intento en menos de un segundo requiere ejecución y medición con relay; el encolado no demuestra entrega.

##### Dependencies & Assumptions
Ejecución futura con CU-04 implementado y dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java); no demuestran la orquestación completa.

##### Preconditions & Default Setup
Reservas RESERVED con m1_blocked verdadero; causas logística y mecánica en solicitudes independientes.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Cancelar como propietario y observar mensajes encolados para M1. | Datos de las precondiciones | La causa logística solicita AVAILABLE; avería solicita MAINTENANCE_CLEANING; otras reservas futuras permanecen intactas. | Pendiente | ModuleNotificationMatrixTest#cancelledAfterReservedNotifiesAvailable pasó: matriz unitaria decide AVAILABLE; ModuleNotificationMatrixTest#cancelledByOwnerWithMechanicalFailureNotifiesMaintenance pasó: matriz unitaria decide MAINTENANCE_CLEANING; falta flujo de cancelación y entrega. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin publicación real |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
El primer intento en menos de un segundo requiere ejecución y medición con relay; el encolado no demuestra entrega.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Blocked; la evidencia solo cubre el componente compartido, el flujo está bloqueado.

#### Traceability
FR-010, FR-011, FR-012 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledAfterReservedNotifiesAvailable; src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java#cancelledByOwnerWithMechanicalFailureNotifiesMaintenance | Blocked
