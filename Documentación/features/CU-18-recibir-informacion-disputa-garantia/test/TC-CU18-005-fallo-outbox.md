### Test Case Specification: TC-CU18-005 — Atomicidad de transición y outbox
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU18-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar atomicidad de transición y outbox. Requisitos: FR-001, FR-005. Criterios: SC-001, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La transición y el outbox están conectados transaccionalmente en código; no se identificó prueba que fuerce un fallo de inserción de outbox de disputa y compruebe el rollback persistido.

##### Dependencies & Assumptions
La evidencia identifica las dependencias usadas; un broker de prueba no demuestra consumo por M3.

##### Preconditions & Default Setup
Disputa PENDING y fallo simulado al persistir evento final.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente de fallo de inserción del outbox de disputa con verificación de rollback. |
| 2 | Ejecutar | Resolver disputa y forzar fallo de outbox. | Datos de las precondiciones | El plan exige rollback de la transición completa si no se puede escribir el evento. | Pendiente | Sin ejecución automatizada pertinente de fallo de inserción del outbox de disputa con verificación de rollback. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente de fallo de inserción del outbox de disputa con verificación de rollback. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La topología de cola y el flujo de publicación existen; falta prueba de rollback al fallar la escritura del outbox.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Pendiente; sin ejecución automatizada pertinente. La transición y el outbox están conectados transaccionalmente en código; no se identificó prueba que fuerce un fallo de inserción de outbox de disputa y compruebe el rollback persistido.

#### Traceability
FR-001, FR-005 | SC-001, SC-004 | Sin prueba automatizada pertinente | In Process
