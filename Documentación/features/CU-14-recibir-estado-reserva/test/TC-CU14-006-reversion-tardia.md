### Test Case Specification: TC-CU14-006 — Reversión de cobro tardío
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU14-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reversión de cobro tardío. Requisitos: FR-001. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La emisión se inicia desde CU-08 y usa outbox; no hay respuesta de M3. CU-13 y el mapeo de reversión existen; falta comprobar entrega a M3 real.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: Sin prueba de CU-14; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva EXPIRED y aprobación tardía informada por CU-13.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Procesar resultado tardío y observar evento para M3. | Datos de las precondiciones | El plan propone publicar CANCELLED_FLEXIBLE para solicitar reversión sin cambiar EXPIRED. | Pendiente | Integración: ConfirmPaymentIntegrationTest#approvedOnExpiredReservationPublishesReversal pasó: EXPIRED se conserva y se encola evento de reversión en puerto simulado. Falta verificar payload y entrega real a M3. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-13 y el mapeo de reversión existen; falta comprobar entrega a M3 real.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial. Consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-001 | SC-001 | Prueba de reversión ejecutada | Evidencia: src/test/java/com/seashare/m2/reservas/integration/ConfirmPaymentIntegrationTest.java#approvedOnExpiredReservationPublishesReversal | In Process
