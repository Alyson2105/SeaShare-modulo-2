### Test Case Specification: TC-CU07-005 — Apertura de disputa
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar apertura de disputa. Requisitos: FR-004, FR-010. Criterios: SC-002, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La apertura directa pasa; EndNavigationService#execute no invoca CU-16 y devuelve campos de disputa nulos. El endpoint y servicio existen. CU-16 existe, pero CU-07 no lo invoca; la retención financiera pertenece a M3.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-07.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION válida con CU-16 disponible y reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde. La apertura directa pasa; EndNavigationService#execute no invoca CU-16 y devuelve campos de disputa nulos. |
| 2 | Ejecutar | Confirmar cierre y consultar disputa asociada. | Datos de las precondiciones | Plan prevé disputa PENDING y ventana de 24 horas desde el cierre, con dispute_id y vencimiento. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde. La apertura directa pasa; EndNavigationService#execute no invoca CU-16 y devuelve campos de disputa nulos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | OpenDisputeIntegrationTest#opensPendingDisputeWith24HourWindow: verde. La apertura directa pasa; EndNavigationService#execute no invoca CU-16 y devuelve campos de disputa nulos. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-16 existe, pero CU-07 no lo invoca; la retención financiera pertenece a M3.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Blocked; evidencia parcial.

#### Traceability
FR-004, FR-010 | SC-002, SC-004 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/OpenDisputeIntegrationTest.java#opensPendingDisputeWith24HourWindow | Blocked
