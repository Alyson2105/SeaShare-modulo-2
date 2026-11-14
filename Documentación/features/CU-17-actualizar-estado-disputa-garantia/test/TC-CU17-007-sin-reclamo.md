### Test Case Specification: TC-CU17-007 — Resolución sin reclamo
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar resolución sin reclamo. Requisitos: FR-001, FR-002. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El spec CU-17, edge case actualización sin reclamo y D-01, deja sin decidir si el Admin puede resolver una disputa PENDING sin reclamo.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING sin reclamo y ventana vigente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | UpdateDisputeStatusServiceTest#adminCannotAcceptWithoutClaim (XML V4: passed). La prueba con puertos simulados comprueba INVALID_RESERVATION_STATE al intentar ACCEPTED y ausencia de revisión; no determina el contrato ni persistencia real. |
| 2 | Ejecutar | Intentar resolver como ADMIN. | Datos de las precondiciones | El resultado requiere definición del contrato. | Pendiente | UpdateDisputeStatusServiceTest#adminCannotAcceptWithoutClaim (XML V4: passed). La prueba con puertos simulados comprueba INVALID_RESERVATION_STATE al intentar ACCEPTED y ausencia de revisión; no determina el contrato ni persistencia real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | UpdateDisputeStatusServiceTest#adminCannotAcceptWithoutClaim (XML V4: passed). La prueba con puertos simulados comprueba INVALID_RESERVATION_STATE al intentar ACCEPTED y ausencia de revisión; no determina el contrato ni persistencia real. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Spec D-01 no define si se admite resolución antes de presentar reclamo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: UpdateDisputeStatusServiceTest#adminCannotAcceptWithoutClaim. El spec CU-17, edge case actualización sin reclamo y D-01, deja sin decidir si el Admin puede resolver una disputa PENDING sin reclamo.

#### Traceability
FR-001, FR-002 | SC-001 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#adminCannotAcceptWithoutClaim; src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#adminCanRejectWithoutClaim | Draft
