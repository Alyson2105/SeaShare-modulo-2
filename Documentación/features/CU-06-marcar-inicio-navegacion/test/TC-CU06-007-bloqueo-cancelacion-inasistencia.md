### Test Case Specification: TC-CU06-007 — Inadmisibilidad después del inicio
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-007

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar inadmisibilidad después del inicio. Requisitos: FR-009. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Verifica rechazo de cancelación tras inicio; no ejecuta inicio seguido de no-show. El endpoint y servicio existen. CU-04 y CU-05 tienen endpoints; la prueba no demuestra todos los flujos.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-06.

##### Preconditions & Default Setup
Reserva que pasó a IN_NAVIGATION.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NavigationStartIntegrationTest#renterCancellationAfterStartIsRejected: verde. Verifica rechazo de cancelación tras inicio; no ejecuta inicio seguido de no-show. |
| 2 | Ejecutar | Intentar cancelación y reporte de inasistencia en solicitudes separadas. | Datos de las precondiciones | Ambas operaciones se rechazan sin transición adicional. | Pendiente | NavigationStartIntegrationTest#renterCancellationAfterStartIsRejected: verde. Verifica rechazo de cancelación tras inicio; no ejecuta inicio seguido de no-show. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationStartIntegrationTest#renterCancellationAfterStartIsRejected: verde. Verifica rechazo de cancelación tras inicio; no ejecuta inicio seguido de no-show. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-04 y CU-05 tienen endpoints; la prueba no demuestra todos los flujos.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-009 | SC-003 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationStartIntegrationTest.java#renterCancellationAfterStartIsRejected | In Process
