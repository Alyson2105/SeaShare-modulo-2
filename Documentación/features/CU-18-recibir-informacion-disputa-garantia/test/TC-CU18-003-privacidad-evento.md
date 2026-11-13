### Test Case Specification: TC-CU18-003 — Contenido del evento
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU18-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar contenido del evento. Requisitos: FR-002, FR-003, FR-006. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-18 spec FR-002 permite motivo opcional en REJECTED; plan CU-18 y specs CU-16/CU-17 prohíben transmitirlo. Resolver antes de cerrar contrato.

##### Dependencies & Assumptions
La evidencia identifica las dependencias usadas; un broker de prueba no demuestra consumo por M3.

##### Preconditions & Default Setup
Disputa final con motivo administrativo y datos de reclamo.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts: verde; verifica cinco campos y ausencia de reason, owner_id y amount en ACCEPTED, sin inspeccionar rechazo administrativo. |
| 2 | Ejecutar | Inspeccionar payload serializado dirigido a M3. | Datos de las precondiciones | No incluye montos, instrucciones de pago ni datos de pasarela. | Pendiente | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts: verde; verifica cinco campos y ausencia de reason, owner_id y amount en ACCEPTED, sin inspeccionar rechazo administrativo. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts: verde; verifica cinco campos y ausencia de reason, owner_id y amount en ACCEPTED, sin inspeccionar rechazo administrativo. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-18 FR-002 permite motivo opcional, pero CU-16, CU-17 y el plan de CU-18 prohíben enviarlo; resolver contrato.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Draft; evidencia parcial.

#### Traceability
FR-002, FR-003, FR-006 | SC-002 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#acceptedIsPublishedAsCompletedWithoutAmounts | Draft
