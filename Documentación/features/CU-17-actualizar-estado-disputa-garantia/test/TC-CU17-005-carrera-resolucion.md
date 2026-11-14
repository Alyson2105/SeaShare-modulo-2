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
El spec CU-17, edge case actualización sin reclamo y D-01, no define si el Admin puede resolver PENDING sin reclamo antes de vencer; el plan sección 7.3 exige una carrera Admin/job. El test simula conflicto y no ejecuta transacciones concurrentes.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING susceptible de vencimiento y resolución administrativa simultáneos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved (XML V4: passed). El repositorio simulado lanza OptimisticLockingFailureException y se comprueba DISPUTE_ALREADY_RESOLVED; no hay dos transacciones reales ni comprobación de ganador y efectos. |
| 2 | Ejecutar | Coordinar ambas transiciones. | Datos de las precondiciones | Solo una transacción se confirma; la perdedora no altera el estado final. | Pendiente | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved (XML V4: passed). El repositorio simulado lanza OptimisticLockingFailureException y se comprueba DISPUTE_ALREADY_RESOLVED; no hay dos transacciones reales ni comprobación de ganador y efectos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved (XML V4: passed). El repositorio simulado lanza OptimisticLockingFailureException y se comprueba DISPUTE_ALREADY_RESOLVED; no hay dos transacciones reales ni comprobación de ganador y efectos. |

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
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: UpdateDisputeStatusServiceTest#optimisticLockBecomesAlreadyResolved. El spec CU-17, edge case actualización sin reclamo y D-01, no define si el Admin puede resolver PENDING sin reclamo antes de vencer; el plan sección 7.3 exige una carrera Admin/job. El test simula conflicto y no ejecuta transacciones concurrentes.

#### Traceability
FR-002, FR-003, FR-006 | SC-001, SC-002 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#optimisticLockBecomesAlreadyResolved | Draft
