### Test Case Specification: TC-CU17-003 — Inmutabilidad terminal
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU17-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar inmutabilidad terminal. Requisitos: FR-003. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Se comprueba ACCEPTED en el servicio simulado y el trigger SQL para REJECTED; faltan la matriz HTTP y la comparación persistida completa para ambos estados.

##### Dependencies & Assumptions
Se emplean las dependencias señaladas en la evidencia; los mocks y servicios externos no acreditan el flujo completo.

##### Preconditions & Default Setup
Disputas ACCEPTED y REJECTED independientes.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | DisputeFinalStateImmutabilityTest#updateOnFinalDisputeFailsAndRowStaysIntact (XML V4: passed). Dos intentos SQL de UPDATE sobre REJECTED lanzan DataAccessException; el método no consulta después la fila ni prueba ACCEPTED en PostgreSQL. |
| 2 | Ejecutar | Intentar cambiar cada estado terminal. | Datos de las precondiciones | Según el plan, 409 DISPUTE_ALREADY_RESOLVED sin mutación. | Pendiente | DisputeFinalStateImmutabilityTest#updateOnFinalDisputeFailsAndRowStaysIntact (XML V4: passed). Dos intentos SQL de UPDATE sobre REJECTED lanzan DataAccessException; el método no consulta después la fila ni prueba ACCEPTED en PostgreSQL. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | DisputeFinalStateImmutabilityTest#updateOnFinalDisputeFailsAndRowStaysIntact (XML V4: passed). Dos intentos SQL de UPDATE sobre REJECTED lanzan DataAccessException; el método no consulta después la fila ni prueba ACCEPTED en PostgreSQL. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
También aplica al cierre automático del sistema.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: DisputeFinalStateImmutabilityTest#updateOnFinalDisputeFailsAndRowStaysIntact. Se comprueba ACCEPTED en el servicio simulado y el trigger SQL para REJECTED; faltan la matriz HTTP y la comparación persistida completa para ambos estados.

#### Traceability
FR-003 | SC-002 | Evidencia: src/test/java/com/seashare/m2/disputa/application/service/UpdateDisputeStatusServiceTest.java#finalDisputeCannotBeChanged; src/test/java/com/seashare/m2/disputa/integration/DisputeFinalStateImmutabilityTest.java#updateOnFinalDisputeFailsAndRowStaysIntact | In Process
