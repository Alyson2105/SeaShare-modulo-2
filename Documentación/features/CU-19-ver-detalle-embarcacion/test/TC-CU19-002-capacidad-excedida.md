### Test Case Specification: TC-CU19-002 — Capacidad excedida
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU19-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar capacidad excedida. Requisitos: FR-004, FR-005. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
VesselDetailControllerTest#capacityExceededIs409 aserta HTTP 409 y código por separado; SC-002 exige bloqueo en interfaz que no se prueba.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Pruebas parciales de componentes: [flota](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java); no demuestran el flujo completo de CU-19.

##### Preconditions & Default Setup
M1 devuelve capacidad conocida; pasajeros solicitados superan el máximo.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | VesselDetailServiceTest#capacityExceededIsRejectedBeforeM3 (XML V4: passed). Aserta CAPACITY_EXCEEDED y verifica que no se invoque cotización con puertos simulados. VesselDetailControllerTest#capacityExceededIs409 aserta HTTP 409 y código por separado; SC-002 exige bloqueo en interfaz que no se prueba. |
| 2 | Ejecutar | Solicitar cotización de detalle y observar consultas a M3. | Datos de las precondiciones | Según el plan, 409 CAPACITY_EXCEEDED antes de invocar M3. | Pendiente | VesselDetailServiceTest#capacityExceededIsRejectedBeforeM3 (XML V4: passed). Aserta CAPACITY_EXCEEDED y verifica que no se invoque cotización con puertos simulados. VesselDetailControllerTest#capacityExceededIs409 aserta HTTP 409 y código por separado; SC-002 exige bloqueo en interfaz que no se prueba. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | VesselDetailServiceTest#capacityExceededIsRejectedBeforeM3 (XML V4: passed). Aserta CAPACITY_EXCEEDED y verifica que no se invoque cotización con puertos simulados. VesselDetailControllerTest#capacityExceededIs409 aserta HTTP 409 y código por separado; SC-002 exige bloqueo en interfaz que no se prueba. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
SC-002 exige bloqueo UI; este caso solo describe validación backend.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: VesselDetailServiceTest#capacityExceededIsRejectedBeforeM3. VesselDetailControllerTest#capacityExceededIs409 aserta HTTP 409 y código por separado; SC-002 exige bloqueo en interfaz que no se prueba.

#### Traceability
FR-004, FR-005 | SC-002 | [flota](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/fleet/HttpFleetClientTest.java); [cotizacion](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpIndividualEstimateAdapterTest.java)  | src/test/java/com/seashare/m2/reservas/application/service/VesselDetailServiceTest.java#capacityExceededIsRejectedBeforeM3; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselDetailControllerTest.java#capacityExceededIs409 | In Process
