### Test Case Specification: TC-CU17-002 — Revisión en curso
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar revisión en curso. Requisitos: FR-001, FR-002, FR-007. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El SLA de 24 horas requiere medición del flujo completo; la identidad administrativa usa cabeceras de prueba, no JWT.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING con revisión administrativa sin terminar.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | UpdateDisputeStatusServiceTest#adminKeepsPendingWithoutTransition y DisputePublishingIntegrationTest#pendingDisputeNeverPublishes: verdes; prueban revisión con repositorio simulado y ausencia de mensaje en RabbitMQ. No se verifica persistencia real de la nota. |
| 2 | Ejecutar | Actualizar a PENDING con nota interna. | Datos de las precondiciones | Se conserva PENDING y se registra actividad sin evento final a M3. | Pendiente | UpdateDisputeStatusServiceTest#adminKeepsPendingWithoutTransition y DisputePublishingIntegrationTest#pendingDisputeNeverPublishes: verdes; prueban revisión con repositorio simulado y ausencia de mensaje en RabbitMQ. No se verifica persistencia real de la nota. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | UpdateDisputeStatusServiceTest#adminKeepsPendingWithoutTransition y DisputePublishingIntegrationTest#pendingDisputeNeverPublishes: verdes; prueban revisión con repositorio simulado y ausencia de mensaje en RabbitMQ. No se verifica persistencia real de la nota. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El SLA de 24 horas requiere medición del flujo completo.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001, FR-002, FR-007 | SC-001 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#adminKeepsPendingWithoutTransition; src/test/java/com/seashare/m2/disputa/integration/DisputePublishingIntegrationTest.java#pendingDisputeNeverPublishes | In Process
