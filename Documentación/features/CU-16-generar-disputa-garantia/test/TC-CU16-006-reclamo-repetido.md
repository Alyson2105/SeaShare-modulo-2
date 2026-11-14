### Test Case Specification: TC-CU16-006 — Segundo reclamo o edición
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar segundo reclamo o edición. Requisitos: FR-006. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El spec CU-16, edge case segundo reclamo y duda D-01, no define segundo registro ni edición; el plan propone 409 CLAIM_ALREADY_EXISTS. El resultado esperado para edición sigue indeterminado.

##### Dependencies & Assumptions
La evidencia V4 y sus límites se indican en Comments.

##### Preconditions & Default Setup
Disputa PENDING con reclamo ya registrado y ventana aún vigente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | RegisterDisputeClaimIntegrationTest#rejectsSecondClaim (XML V4: passed). Comprueba la excepción CLAIM_ALREADY_EXISTS al segundo registro; no comprueba edición ni postcondiciones persistidas completas. |
| 2 | Ejecutar | Intentar segundo registro o edición. | Datos de las precondiciones | El plan prevé 409 CLAIM_ALREADY_EXISTS para segundo registro; el spec mantiene abierta la política. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsSecondClaim (XML V4: passed). Comprueba la excepción CLAIM_ALREADY_EXISTS al segundo registro; no comprueba edición ni postcondiciones persistidas completas. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | RegisterDisputeClaimIntegrationTest#rejectsSecondClaim (XML V4: passed). Comprueba la excepción CLAIM_ALREADY_EXISTS al segundo registro; no comprueba edición ni postcondiciones persistidas completas. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
No está definido en spec si se admite edición u otro reclamo; resolver D-01 antes de cerrar el caso.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: RegisterDisputeClaimIntegrationTest#rejectsSecondClaim. El spec CU-16, edge case segundo reclamo y duda D-01, no define segundo registro ni edición; el plan propone 409 CLAIM_ALREADY_EXISTS. El resultado esperado para edición sigue indeterminado.

#### Traceability
FR-006 | SC-005 | src/test/java/com/seashare/m2/disputa/integration/RegisterDisputeClaimIntegrationTest.java#rejectsSecondClaim | Draft
