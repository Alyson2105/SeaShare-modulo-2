### Test Case Specification: TC-CU01-004 — Rechazo de formato inválido
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU01-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo de formato inválido. Requisitos: FR-001, FR-010. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Spec excluye reglas de negocio de fechas; no asumir rechazo por fecha pasada o rango invertido.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: el flujo completo con proveedores reales queda pendiente.

##### Preconditions & Default Setup
Dobles observables para proveedores M1 y M3.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Enviar fecha malformada, pasajeros no numéricos y tipo inválido en solicitudes separadas. | Datos de las precondiciones | Según plan, 400 INVALID_PARAMETERS sin invocar proveedores. | Pendiente | Componente MockMvc: VesselSearchControllerTest#invalidDateIsRejectedWithoutCallingModuleOne, #invalidTypeIsRejected y #invalidPassengersIsRejected pasaron con 400 e INVALID_PARAMETERS; pasajeros probados con cero. No se verifican explícitamente las invocaciones a proveedores ni el caso no numérico. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Spec excluye reglas de negocio de fechas; no asumir rechazo por fecha pasada o rango invertido.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial. Cobertura parcial; faltan las comprobaciones indicadas en el flujo.

#### Traceability
FR-001, FR-010 | SC-004 | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#invalidDateIsRejectedWithoutCallingModuleOne; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#invalidTypeIsRejected; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/VesselSearchControllerTest.java#invalidPassengersIsRejected | In Process
