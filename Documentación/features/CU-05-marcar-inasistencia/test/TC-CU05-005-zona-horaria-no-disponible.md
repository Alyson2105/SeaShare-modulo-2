### Test Case Specification: TC-CU05-005 — Fallo de zona horaria
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU05-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo de zona horaria. Requisitos: FR-004, FR-012. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Prueba zona almacenada inválida y 503 HARBOR_TIMEZONE_UNAVAILABLE; no existe consulta a CU-09 durante el reporte, requerida por plan para FLEET_SERVICE_UNAVAILABLE. El endpoint y servicio existen. Evaluar también zona inválida conforme se defina el contrato.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-05.

##### Preconditions & Default Setup
Reserva RESERVED; CU-09 falla o no proporciona zona horaria.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NoShowIntegrationTest#invalidHarborTimezoneReturns503: verde. Prueba zona almacenada inválida y 503 HARBOR_TIMEZONE_UNAVAILABLE; no existe consulta a CU-09 durante el reporte, requerida por plan para FLEET_SERVICE_UNAVAILABLE. |
| 2 | Ejecutar | Intentar reportar inasistencia. | Datos de las precondiciones | Según el plan, 503 FLEET_SERVICE_UNAVAILABLE; no asumir zona por defecto ni cambiar la reserva. | Pendiente | NoShowIntegrationTest#invalidHarborTimezoneReturns503: verde. Prueba zona almacenada inválida y 503 HARBOR_TIMEZONE_UNAVAILABLE; no existe consulta a CU-09 durante el reporte, requerida por plan para FLEET_SERVICE_UNAVAILABLE. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NoShowIntegrationTest#invalidHarborTimezoneReturns503: verde. Prueba zona almacenada inválida y 503 HARBOR_TIMEZONE_UNAVAILABLE; no existe consulta a CU-09 durante el reporte, requerida por plan para FLEET_SERVICE_UNAVAILABLE. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Evaluar también zona inválida conforme se defina el contrato.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Blocked; evidencia parcial.

#### Traceability
FR-004, FR-012 | SC-001 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NoShowIntegrationTest.java#invalidHarborTimezoneReturns503 | Blocked
