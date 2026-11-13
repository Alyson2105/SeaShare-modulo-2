### Test Case Specification: TC-CU16-001 — Apertura automática
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar apertura automática. Requisitos: FR-001, FR-003, FR-004, FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-07 no invoca OpenDisputeUseCase; la apertura directa persiste PENDING y 24 h, pero no acredita el disparador automático ni la operación inicial mediante CU-17.

##### Dependencies & Assumptions
La prueba usa componentes controlados; no demuestra el flujo completo.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION y cierre CU-07 válido; reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde; invoca apertura directamente, no desde CU-07. |
| 2 | Ejecutar | Finalizar navegación e inspeccionar disputa asociada. | Datos de las precondiciones | Se crea exactamente una disputa PENDING por el sistema mediante CU-17 con vencimiento 24 horas después. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde; invoca apertura directamente, no desde CU-07. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde; invoca apertura directamente, no desde CU-07. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Estado de disputa y estado de reserva son entidades diferentes.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Blocked; evidencia parcial, pasos completos pendientes.

#### Traceability
FR-001, FR-003, FR-004, FR-005 | SC-001 | src/test/java/com/seashare/m2/disputa/integration/OpenDisputeIntegrationTest.java#opensPendingDisputeWith24HourWindow | Blocked
