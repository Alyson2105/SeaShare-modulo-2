### Test Case Specification: TC-CU01-001 — Búsqueda con filtros
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU01-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar búsqueda con filtros. Requisitos: FR-001, FR-002, FR-003. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Las pruebas de servicio aíslan tipo y capacidad con puertos simulados; faltan la combinación de fechas, pasajeros y tipo y la respuesta HTTP completa.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java); no demuestran el flujo completo de CU-01.

##### Preconditions & Default Setup
Catálogo simulado M1 y cotización lote simulada M3 con valores conocidos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | VesselSearchServiceTest#filtersByTypeLocally (XML V4: passed). Aserta que solo queda YACHT y que su tarifa simulada es 100; filtersByCapacity comprueba el filtro de capacidad por separado. |
| 2 | Ejecutar | Consultar GET /api/v1/vessels con fechas, pasajeros y tipo válidos. | Datos de las precondiciones | El plan prevé 200 con embarcaciones coincidentes y estimaciones de M3 sin cálculo local. | Pendiente | VesselSearchServiceTest#filtersByTypeLocally (XML V4: passed). Aserta que solo queda YACHT y que su tarifa simulada es 100; filtersByCapacity comprueba el filtro de capacidad por separado. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | VesselSearchServiceTest#filtersByTypeLocally (XML V4: passed). Aserta que solo queda YACHT y que su tarifa simulada es 100; filtersByCapacity comprueba el filtro de capacidad por separado. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Existen endpoint y orquestación; las pruebas usan dobles de catálogo y cotización.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: VesselSearchServiceTest#filtersByTypeLocally. Las pruebas de servicio aíslan tipo y capacidad con puertos simulados; faltan la combinación de fechas, pasajeros y tipo y la respuesta HTTP completa.

#### Traceability
FR-001, FR-002, FR-003 | SC-001 | [lote](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpBatchEstimateAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java#filtersByTypeLocally; src/test/java/com/seashare/m2/reservas/application/service/VesselSearchServiceTest.java#filtersByCapacity | In Process
