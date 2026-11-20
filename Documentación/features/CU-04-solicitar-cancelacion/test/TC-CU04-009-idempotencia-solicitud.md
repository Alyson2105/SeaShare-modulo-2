### Test Case Specification: TC-CU04-009 — Repetición idempotente
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-009

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar repetición idempotente. Trazabilidad: FR-011, FR-015; SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El plan CU-04 sección 7.5 exige respuesta 200 idéntica con la misma clave; el mismo plan, DIFERENCIA H8-CU04-05, declara esa idempotencia fuera de la primera versión. CancellationController no procesa la clave y no hay prueba pertinente.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED; solicitud válida y X-Idempotency-Key UUID según el plan.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | Sin ejecución automatizada pertinente de repetición con clave. El contrato contiene disposiciones incompatibles sobre el alcance. |
| 2 | Ejecutar | Repetir la misma solicitud con la misma clave dentro de 60 segundos. | Datos de las precondiciones | El plan prevé 200 con respuesta original, sin duplicar transición ni evento. | Pendiente | Sin ejecución automatizada pertinente de repetición con clave. El contrato contiene disposiciones incompatibles sobre el alcance. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin ejecución automatizada pertinente de repetición con clave. El contrato contiene disposiciones incompatibles sobre el alcance. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La idempotencia implementada para creación CU-02 no prueba idempotencia de cancelación.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Evaluado el 2026-11-13: Draft; sin ejecución automatizada pertinente. El plan CU-04 sección 7.5 exige respuesta 200 idéntica con la misma clave; el mismo plan, DIFERENCIA H8-CU04-05, declara esa idempotencia fuera de la primera versión. CancellationController no procesa la clave y no hay prueba pertinente.

#### Traceability
FR-011, FR-015 | SC-001 | Sin prueba automatizada pertinente | Draft
