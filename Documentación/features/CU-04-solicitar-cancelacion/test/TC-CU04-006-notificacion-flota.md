### Test Case Specification: TC-CU04-006 — Destino operativo M1
**Status**: In Process
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
Existe decisión y encolado operativo; el adaptador M1 es fake. La entrega y el primer intento en menos de un segundo requieren medición.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reservas RESERVED con m1_blocked verdadero; causas logística y mecánica en solicitudes independientes.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | RequestCancellationServiceTest#ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance: verde con CU-08 simulado; no acredita entrega M1 ni reservas futuras. |
| 2 | Ejecutar | Cancelar como propietario y observar mensajes encolados para M1. | Datos de las precondiciones | La causa logística solicita AVAILABLE; avería solicita MAINTENANCE_CLEANING; otras reservas futuras permanecen intactas. | Pendiente | RequestCancellationServiceTest#ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance: verde con CU-08 simulado; no acredita entrega M1 ni reservas futuras. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | RequestCancellationServiceTest#ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance: verde con CU-08 simulado; no acredita entrega M1 ni reservas futuras. |

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
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-010, FR-011, FR-012 | SC-005 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [matriz](../../../../src/test/java/com/seashare/m2/reservas/application/service/ModuleNotificationMatrixTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#ownerWithMechanicalFailureAndM1BlockedNotifiesMaintenance | In Process
