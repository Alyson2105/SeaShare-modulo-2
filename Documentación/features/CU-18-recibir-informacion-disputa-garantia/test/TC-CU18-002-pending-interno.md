### Test Case Specification: TC-CU18-002 — Ausencia de evento PENDING
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU18-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar ausencia de evento pending. Requisitos: FR-001. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El outbox y la publicación existen; ausencia de mensaje en cola después del relay no demuestra por sí sola ausencia de fila al crear.

##### Dependencies & Assumptions
La evidencia identifica las dependencias usadas; un broker de prueba no demuestra consumo por M3.

##### Preconditions & Default Setup
Nueva disputa creada en PENDING.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputePublishingIntegrationTest#pendingDisputeNeverPublishes: verde tras abrir, reclamar, mantener PENDING y ejecutar relay; no inspecciona directamente el outbox al crear. |
| 2 | Ejecutar | Inspeccionar outbox asociado a la creación. | Datos de las precondiciones | No se encola evento de disputa hacia M3 al crear PENDING. | Pendiente | DisputePublishingIntegrationTest#pendingDisputeNeverPublishes: verde tras abrir, reclamar, mantener PENDING y ejecutar relay; no inspecciona directamente el outbox al crear. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputePublishingIntegrationTest#pendingDisputeNeverPublishes: verde tras abrir, reclamar, mantener PENDING y ejecutar relay; no inspecciona directamente el outbox al crear. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El contexto de disputa y su outbox existen; no se consultó directamente la fila al crear PENDING.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001 | SC-001 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#pendingDisputeNeverPublishes; src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#adminKeepsPendingWithoutTransition | In Process
