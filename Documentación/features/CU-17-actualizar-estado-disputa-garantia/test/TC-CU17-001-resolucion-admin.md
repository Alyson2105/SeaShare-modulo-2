### Test Case Specification: TC-CU17-001 — Resolución administrativa
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar resolución administrativa. Requisitos: FR-001, FR-002, FR-004, FR-005. Criterios: SC-001, SC-003, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Endpoint y servicio existen. La notificación final depende de CU-18; no implica liquidación confirmada.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputa PENDING con reclamo y usuario ADMIN autorizado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | UpdateDisputeStatusServiceTest#adminAcceptsDisputeWithClaim y #adminRejectsWithReason: verdes con repositorio y outbox simulados; controlador prueba 200 con caso de uso simulado. No se demuestra toda la combinación de motivos y destinos con persistencia y JWT reales. |
| 2 | Ejecutar | Resolver a ACCEPTED y REJECTED en disputas separadas, con y sin motivo. | Datos de las precondiciones | El plan prevé 200, estado final correspondiente y motivo opcional local, sin cálculos monetarios. | Pendiente | UpdateDisputeStatusServiceTest#adminAcceptsDisputeWithClaim y #adminRejectsWithReason: verdes con repositorio y outbox simulados; controlador prueba 200 con caso de uso simulado. No se demuestra toda la combinación de motivos y destinos con persistencia y JWT reales. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | UpdateDisputeStatusServiceTest#adminAcceptsDisputeWithClaim y #adminRejectsWithReason: verdes con repositorio y outbox simulados; controlador prueba 200 con caso de uso simulado. No se demuestra toda la combinación de motivos y destinos con persistencia y JWT reales. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La notificación final depende de CU-18; no implica liquidación confirmada.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001, FR-002, FR-004, FR-005 | SC-001, SC-003, SC-004 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#adminAcceptsDisputeWithClaim; src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#adminRejectsWithReason; src/test/java/com/seashare/m2/disputa/infrastructure/adapter/in/web/DisputeAdminControllerTest.java#acceptedReturns200WithContractFields | In Process
