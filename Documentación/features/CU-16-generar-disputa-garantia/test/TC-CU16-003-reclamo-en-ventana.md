### Test Case Specification: TC-CU16-003 — Reclamo dentro de ventana
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU16-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reclamo dentro de ventana. Requisitos: FR-005, FR-006, FR-010, FR-011. Criterios: SC-004, SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El endpoint y el servicio existen. Las pruebas separadas no acreditan el recorrido completo autenticado ni la ausencia integral de operaciones financieras.

##### Dependencies & Assumptions
La prueba usa componentes controlados; no demuestra el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING con ventana vigente y propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | RegisterDisputeClaimIntegrationTest#registersClaimInsideWindowAndKeepsPending: verde con PostgreSQL y reloj simulado; no prueba HTTP ni publicación. |
| 2 | Ejecutar | Registrar reclamo válido antes de window_end. | Datos de las precondiciones | Plan prevé 201; reclamo queda asociado, disputa permanece PENDING, se desactiva cierre automático y no se publica estado PENDING a M3. | Pendiente | RegisterDisputeClaimIntegrationTest#registersClaimInsideWindowAndKeepsPending: verde con PostgreSQL y reloj simulado; no prueba HTTP ni publicación. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | RegisterDisputeClaimIntegrationTest#registersClaimInsideWindowAndKeepsPending: verde con PostgreSQL y reloj simulado; no prueba HTTP ni publicación. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El plan limita description a 10–2000 caracteres; no ejecutar operaciones financieras en M2.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial, pasos completos pendientes.

#### Traceability
FR-005, FR-006, FR-010, FR-011 | SC-004, SC-005 | src/test/java/com/seashare/m2/disputa/integration/RegisterDisputeClaimIntegrationTest.java#registersClaimInsideWindowAndKeepsPending | In Process
