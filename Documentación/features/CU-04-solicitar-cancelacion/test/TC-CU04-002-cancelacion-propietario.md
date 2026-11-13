### Test Case Specification: TC-CU04-002 — Clasificación por propietario
**Status**: Blocked
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
CU-04 y el endpoint propuesto aún no están implementados. Los resultados son contractuales, no observados. La captura visual del motivo es UI.

##### Dependencies & Assumptions
Ejecución futura con CU-04 implementado y dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran la orquestación completa.

##### Preconditions & Default Setup
Reserva RESERVED previa al zarpe, propietario registrado y causa logística válida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Solicitar cancelación como propietario. | Datos de las precondiciones | Según el plan, 200; CANCELLED y BY_OWNER sin clasificación por franja; motivo registrado. | Pendiente | Contrato futuro |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin publicación real |

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
Registrar los resultados cuando el flujo esté disponible; conservar identificadores para la inspección.

#### Traceability
FR-009, FR-010, FR-011 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Blocked
