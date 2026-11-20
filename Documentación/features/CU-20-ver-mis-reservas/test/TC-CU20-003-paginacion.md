### Test Case Specification: TC-CU20-003 — Paginación de reservas
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU20-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar paginación de reservas. Requisitos: FR-012. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
spec.md FR-012 fija diez por página; plan.md permite limit entre 1 y 50. No prueba segunda página ni orden.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Más de diez reservas del mismo usuario con created_at distinguibles.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationQueryTest#invalidPaginationIs400 (XML V4: passed). Aserta 400 INVALID_PARAMETERS para page=0 y limit=51. spec.md FR-012 fija diez por página; plan.md permite limit entre 1 y 50. No prueba segunda página ni orden. |
| 2 | Ejecutar | Consultar páginas 1 y 2 con page y limit; probar page=0 y limit=51 por separado. | Datos de las precondiciones | Resultados ordenados por created_at descendente, items de hasta limit; parámetros inválidos reciben 400 INVALID_PARAMETERS. | Pendiente | ReservationQueryTest#invalidPaginationIs400 (XML V4: passed). Aserta 400 INVALID_PARAMETERS para page=0 y limit=51. spec.md FR-012 fija diez por página; plan.md permite limit entre 1 y 50. No prueba segunda página ni orden. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationQueryTest#invalidPaginationIs400 (XML V4: passed). Aserta 400 INVALID_PARAMETERS para page=0 y limit=51. spec.md FR-012 fija diez por página; plan.md permite limit entre 1 y 50. No prueba segunda página ni orden. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Predeterminado 10, pero código permite hasta 50 mientras FR-012 dice límite fijo de 10.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: ReservationQueryTest#invalidPaginationIs400. spec.md FR-012 fija diez por página; plan.md permite limit entre 1 y 50. No prueba segunda página ni orden.

#### Traceability
FR-012 | SC-002 | [consulta](../../../../src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java)  | src/test/java/com/seashare/m2/reservas/integration/ReservationQueryTest.java#invalidPaginationIs400 | Draft

#### Decisión aprobada — 2026-11-13
Se aprobó limit=10 por defecto y valores configurables de 1 a 50. Esta decisión no modifica automáticamente el veredicto del TC.
