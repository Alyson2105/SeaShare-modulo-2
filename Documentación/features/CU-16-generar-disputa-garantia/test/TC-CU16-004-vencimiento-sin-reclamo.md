### Test Case Specification: TC-CU16-004 — Cierre sin reclamo
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar cierre sin reclamo. Requisitos: FR-007, FR-008, FR-011. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El spec CU-16 FR-011 exige REJECTED → APPROVED, pero plan CU-18 y DisputeEventMapper#finalEvent usan REJECTED → REJECTED; resolver contrato. El barrido de vencimiento sí existe.

##### Dependencies & Assumptions
La prueba usa componentes controlados; no demuestra el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING sin reclamo, con texto informativo de check-out y reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd: verde con PostgreSQL; no parte de CU-07 ni de novedades de check-out. |
| 2 | Ejecutar | Al vencer 24 horas ejecutar cierre automático. | Datos de las precondiciones | Por CU-17 pasa a REJECTED con motivo de sistema; novedades de check-out no cuentan como reclamo; evento final se deriva a CU-18. | Pendiente | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd: verde con PostgreSQL; no parte de CU-07 ni de novedades de check-out. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd: verde con PostgreSQL; no parte de CU-07 ni de novedades de check-out. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Política de reintentos del job sin definir; en PENDING no se publica.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Draft; evidencia parcial, pasos completos pendientes.

#### Traceability
FR-007, FR-008, FR-011 | SC-003 | src/test/java/com/seashare/m2/disputa/integration/DisputeWindowCloseIntegrationTest.java#closesAtExactWindowEnd | Draft
