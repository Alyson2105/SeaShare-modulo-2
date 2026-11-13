### Test Case Specification: TC-CU18-001 — Publicación de estado final
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU18-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar publicación de estado final. Requisitos: FR-001, FR-002. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-18 spec FR-002 pide ACCEPTED/REJECTED; plan CU-18 y DisputeEventMapper#finalEvent usan COMPLETED/REJECTED; CU-16 spec FR-011 pide COMPLETADO/APPROVED.

##### Dependencies & Assumptions
La evidencia identifica las dependencias usadas; un broker de prueba no demuestra consumo por M3.

##### Preconditions & Default Setup
Disputas que alcanzan ACCEPTED y REJECTED por transición válida.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts y #expiryRejectionIsPublishedAsRejected: verdes con PostgreSQL y RabbitMQ; el rechazo no verifica todos los campos y no demuestra consumo por M3. |
| 2 | Ejecutar | Observar eventos asociados a cada resultado final. | Datos de las precondiciones | El plan propone ACCEPTED → COMPLETED y REJECTED → REJECTED con identificadores y fecha. | Pendiente | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts y #expiryRejectionIsPublishedAsRejected: verdes con PostgreSQL y RabbitMQ; el rechazo no verifica todos los campos y no demuestra consumo por M3. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputePublishingIntegrationTest#acceptedIsPublishedAsCompletedWithoutAmounts y #expiryRejectionIsPublishedAsRejected: verdes con PostgreSQL y RabbitMQ; el rechazo no verifica todos los campos y no demuestra consumo por M3. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El spec CU-18 usa ACCEPTED, CU-16 propone COMPLETADO/APPROVED; aclarar mapeo antes de cerrar.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Draft; evidencia parcial.

#### Traceability
FR-001, FR-002 | SC-001 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#acceptedIsPublishedAsCompletedWithoutAmounts; src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#expiryRejectionIsPublishedAsRejected; src/test/java/com/seashare/m2/disputa/application/service/DisputeEventMapperTest.java#acceptedIsPublishedAsCompleted; src/test/java/com/seashare/m2/disputa/application/service/DisputeEventMapperTest.java#rejectedIsPublishedAsRejected | Draft
