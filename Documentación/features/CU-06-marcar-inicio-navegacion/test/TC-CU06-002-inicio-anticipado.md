### Test Case Specification: TC-CU06-002 — Rechazo de salida anticipada
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo de salida anticipada. Requisitos: FR-003. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El spec, Edge Cases «Salida ligeramente antes o después de la hora exacta», admite salida ligeramente anterior; FR-003 del mismo spec y el plan, flujo operativo paso 3, la prohíben. La respuesta contractual no es inequívoca.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-06.

##### Preconditions & Default Setup
Reserva RESERVED del propietario y reloj un segundo antes del zarpe pactado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationStartIntegrationTest#clockBeforeScheduledDepartureReturnsEarlyDeparture (XML V4: passed). Aserta 409 EARLY_DEPARTURE, reserva RESERVED y cero check-in; no comprueba ausencia de notificaciones. |
| 2 | Ejecutar | Solicitar inicio de navegación. | Datos de las precondiciones | Según el plan, 409 EARLY_DEPARTURE; sin transición ni notificaciones. | Pendiente | NavigationStartIntegrationTest#clockBeforeScheduledDepartureReturnsEarlyDeparture (XML V4: passed). Aserta 409 EARLY_DEPARTURE, reserva RESERVED y cero check-in; no comprueba ausencia de notificaciones. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationStartIntegrationTest#clockBeforeScheduledDepartureReturnsEarlyDeparture (XML V4: passed). Aserta 409 EARLY_DEPARTURE, reserva RESERVED y cero check-in; no comprueba ausencia de notificaciones. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El edge case del spec menciona salida ligeramente anterior, en conflicto con FR-003 y el plan; confirmar.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: NavigationStartIntegrationTest#clockBeforeScheduledDepartureReturnsEarlyDeparture. El spec, Edge Cases «Salida ligeramente antes o después de la hora exacta», admite salida ligeramente anterior; FR-003 del mismo spec y el plan, flujo operativo paso 3, la prohíben. La respuesta contractual no es inequívoca.

#### Traceability
FR-003 | SC-002 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationStartIntegrationTest.java#clockBeforeScheduledDepartureReturnsEarlyDeparture | Draft

#### Decisión aprobada — 2026-11-13
Se aprobó rechazar solicitudes y horas reales anteriores al zarpe pactado. Esta decisión no modifica automáticamente el veredicto del TC.
