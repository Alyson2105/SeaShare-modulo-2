### Test Case Specification: TC-CU17-006 — Evento de estado final
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar evento de estado final. Requisitos: FR-004, FR-007. Criterios: SC-003, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-16 spec FR-011 asigna REJECTED → APPROVED, CU-17 spec FR-007 y CU-18 plan REJECTED → REJECTED, y CU-18 spec FR-002 emplea ACCEPTED. La contradicción afecta el payload requerido.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING con reclamo y motivo local opcional.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputePublishingIntegrationTest#expiryRejectionIsPublishedAsRejected (XML V4: passed). Con PostgreSQL y RabbitMQ comprueba un mensaje con status REJECTED y event_key correspondiente; no resuelve la contradicción ni prueba consumo de M3. |
| 2 | Ejecutar | Resolver y examinar mensaje encolado para M3. | Datos de las precondiciones | El evento no incluye motivo ni datos privados; PENDING no genera evento. | Pendiente | DisputePublishingIntegrationTest#expiryRejectionIsPublishedAsRejected (XML V4: passed). Con PostgreSQL y RabbitMQ comprueba un mensaje con status REJECTED y event_key correspondiente; no resuelve la contradicción ni prueba consumo de M3. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputePublishingIntegrationTest#expiryRejectionIsPublishedAsRejected (XML V4: passed). Con PostgreSQL y RabbitMQ comprueba un mensaje con status REJECTED y event_key correspondiente; no resuelve la contradicción ni prueba consumo de M3. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Mapeo entre CU-16, CU-17 y CU-18 presenta valores incompatibles; resolver antes de cerrar contrato.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: DisputePublishingIntegrationTest#expiryRejectionIsPublishedAsRejected. CU-16 spec FR-011 asigna REJECTED → APPROVED, CU-17 spec FR-007 y CU-18 plan REJECTED → REJECTED, y CU-18 spec FR-002 emplea ACCEPTED. La contradicción afecta el payload requerido.

#### Traceability
FR-004, FR-007 | SC-003, SC-004 | Evidencia: src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#acceptedIsPublishedAsCompletedWithoutAmounts; src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#pendingDisputeNeverPublishes; src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#expiryRejectionIsPublishedAsRejected | Draft
