### Test Case Specification: TC-CU17-004 — Cierre automático
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar cierre automático. Requisitos: FR-002, FR-006. Criterios: SC-001, SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La política de reintento del job está pendiente; la prueba ejecuta el barrido directamente. DisputeWindowCloseJob se habilita con m2.scheduler.dispute.enabled=true y está desactivado por defecto.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING sin reclamo y ventana vencida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd: verde con PostgreSQL y reloj simulado; barrido ejecutado directamente, estado REJECTED, motivo, actor SYSTEM e historial persistidos. |
| 2 | Ejecutar | Ejecutar vencimiento de CU-16 mediante la operación interna de CU-17. | Datos de las precondiciones | El sistema transiciona a REJECTED con motivo informativo. | Pendiente | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd: verde con PostgreSQL y reloj simulado; barrido ejecutado directamente, estado REJECTED, motivo, actor SYSTEM e historial persistidos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputeWindowCloseIntegrationTest#closesAtExactWindowEnd: verde con PostgreSQL y reloj simulado; barrido ejecutado directamente, estado REJECTED, motivo, actor SYSTEM e historial persistidos. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La política de reintento del job está pendiente.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; barrido directo con reloj controlado y efectos persistidos; no se verificó ejecución programada.

#### Traceability
FR-002, FR-006 | SC-001, SC-002 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/DisputeWindowCloseIntegrationTest.java#closesAtExactWindowEnd | In Process
