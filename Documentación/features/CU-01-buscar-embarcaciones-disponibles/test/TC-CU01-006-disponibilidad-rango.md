### Test Case Specification: TC-CU01-006 — Filtrado por disponibilidad temporal
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU01-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar filtrado por disponibilidad temporal. Requisitos: FR-002. Criterios: SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La discrepancia afecta resultados y debe resolverse antes de fijar expectativa ejecutable.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: el flujo completo con proveedores reales queda pendiente.

##### Preconditions & Default Setup
Barco con reserva solapada y búsqueda por el mismo rango.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar catálogo con fechas. | Datos de las precondiciones | El spec excluye verificación de disponibilidad por rango; el plan describe filtrado local por solapamiento. | Pendiente | Contrato contradictorio: spec excluye filtro por rango y plan lo exige. VesselSearchControllerTest#overlappingReservationHidesVessel y VesselSearchServiceTest#excludesVesselsWithOverlappingReservations pasaron y confirman el filtrado implementado; no resuelven el contrato. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La discrepancia afecta resultados y debe resolverse antes de fijar expectativa ejecutable.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Draft. El contrato requiere aclaración.

#### Traceability
FR-002 | SC-003 | Sin prueba de CU-01 | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#overlappingReservationHidesVessel; src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java#excludesVesselsWithOverlappingReservations | Draft
