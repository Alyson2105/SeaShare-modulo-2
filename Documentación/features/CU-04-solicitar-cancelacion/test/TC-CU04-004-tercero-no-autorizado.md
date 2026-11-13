### Test Case Specification: TC-CU04-004 — Autorización de actor
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar autorización de actor. Trazabilidad: FR-003; SC-008.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La identidad JWT sigue pendiente; el controlador usa identidad temporal por cabeceras.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED; usuario distinto del arrendatario titular y del propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | RequestCancellationServiceTest#foreignRenterIsRejected: verde con repositorio y CU-08 simulados; no prueba JWT ni persistencia real. |
| 2 | Ejecutar | Solicitar cancelación como tercero. | Datos de las precondiciones | Según el plan, 403 RESERVATION_NOT_OWNED; no se cancela la reserva. | Pendiente | RequestCancellationServiceTest#foreignRenterIsRejected: verde con repositorio y CU-08 simulados; no prueba JWT ni persistencia real. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | RequestCancellationServiceTest#foreignRenterIsRejected: verde con repositorio y CU-08 simulados; no prueba JWT ni persistencia real. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La identidad por JWT es propuesta y aún no implementada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-003 | SC-008 | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#foreignRenterIsRejected | In Process
