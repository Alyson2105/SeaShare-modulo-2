### Test Case Specification: TC-CU17-005 — Carrera con vencimiento
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar carrera con vencimiento. Requisitos: FR-002, FR-003, FR-006. Criterios: SC-001, SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Spec D-01 deja abierta la resolución administrativa sin reclamo; el test simula un conflicto, no la carrera real.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING susceptible de vencimiento y resolución administrativa simultáneos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved: verde al simular conflicto de versión; no ejecuta transacciones concurrentes ni prueba una sola ganadora. |
| 2 | Ejecutar | Coordinar ambas transiciones. | Datos de las precondiciones | Solo una transacción se confirma; la perdedora no altera el estado final. | Pendiente | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved: verde al simular conflicto de versión; no ejecuta transacciones concurrentes ni prueba una sola ganadora. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved: verde al simular conflicto de versión; no ejecuta transacciones concurrentes ni prueba una sola ganadora. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La posibilidad de resolver sin reclamo está abierta en spec D-01.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Draft; evidencia parcial.

#### Traceability
FR-002, FR-003, FR-006 | SC-001, SC-002 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#optimisticLockBecomesAlreadyResolved | Draft
