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
El spec, Edge Cases «Disponibilidad por rango de fechas fuera de alcance», excluye el filtrado; el plan, Notas Transversales «Disponibilidad por rango de fechas y filtrado en M2», lo exige.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: el flujo completo con proveedores reales queda pendiente.

##### Preconditions & Default Setup
Barco con reserva solapada y búsqueda por el mismo rango.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | VesselSearchControllerTest#overlappingReservationHidesVessel (XML V4: passed). MockMvc aserta 200 y lista vacía con solapamiento simulado; no resuelve la contradicción entre contratos. |
| 2 | Ejecutar | Consultar catálogo con fechas. | Datos de las precondiciones | El spec excluye verificación de disponibilidad por rango; el plan describe filtrado local por solapamiento. | Pendiente | VesselSearchControllerTest#overlappingReservationHidesVessel (XML V4: passed). MockMvc aserta 200 y lista vacía con solapamiento simulado; no resuelve la contradicción entre contratos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | VesselSearchControllerTest#overlappingReservationHidesVessel (XML V4: passed). MockMvc aserta 200 y lista vacía con solapamiento simulado; no resuelve la contradicción entre contratos. |

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
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: VesselSearchControllerTest#overlappingReservationHidesVessel. El spec, Edge Cases «Disponibilidad por rango de fechas fuera de alcance», excluye el filtrado; el plan, Notas Transversales «Disponibilidad por rango de fechas y filtrado en M2», lo exige.

#### Traceability
FR-002 | SC-003 | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#overlappingReservationHidesVessel; src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java#excludesVesselsWithOverlappingReservations | Draft
