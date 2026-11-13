### Test Case Specification: TC-CU22-006 — Entrega HTTP a M1
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU22-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar entrega http a m1. Requisitos: FR-006, FR-008. Criterios: SC-001, SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-22 encola destino M1_HTTP; el adaptador actual es fake y no realiza HTTP real. Solo existe adaptador fake; el plan mezcla HTTP y AMQP, falta acordar último salto y semántica de confirmación.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Prueba parcial relacionada: OutboxRelayIntegrationTest#m1HttpUsaElPuertoFalsoYMarcaPublished; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M1 real o simulador contractual disponible y adaptador HTTP implementado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Despachar notificación encolada y medir primer intento. | Datos de las precondiciones | Contrato propone PATCH y entrega con política de reintentos sin revertir transición local. | Pendiente | OutboxRelayIntegrationTest#m1HttpUsaElPuertoFalsoYMarcaPublished pasó: fila insertada manualmente, puerto simulado invocado y published_at persistido; no hubo HTTP ni medición de SLA. El plan mezcla confirmación de broker y PATCH. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Solo existe adaptador fake; el plan mezcla HTTP y AMQP, falta acordar último salto y semántica de confirmación.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Draft; ejecución parcial con puerto falso. OutboxRelayIntegrationTest#m1HttpUsaElPuertoFalsoYMarcaPublished pasó: fila insertada manualmente, puerto simulado invocado y published_at persistido; no hubo HTTP ni medición de SLA. El plan mezcla confirmación de broker y PATCH.

#### Traceability
FR-006, FR-008 | SC-001, SC-002 | Evidencia parcial: src/test/java/com/seashare/m2/reservas/integration/OutboxRelayIntegrationTest.java#m1HttpUsaElPuertoFalsoYMarcaPublished | Draft
