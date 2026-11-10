### Test Case Specification: TC-CU04-005 — Fallo de zona horaria
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU04-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo de zona horaria. Trazabilidad: FR-005, FR-006; SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-04 y el endpoint propuesto aún no están implementados. Los resultados son contractuales, no observados. Probar también hora pactada incompleta; spec exige rechazo descriptivo, sin fijar aquí código.

##### Dependencies & Assumptions
Ejecución futura con CU-04 implementado y dependencias controladas. Pruebas parciales de componentes: Sin prueba de CU-04; no demuestran la orquestación completa.

##### Preconditions & Default Setup
Reserva RESERVED; simular fallo o ausencia de la zona horaria desde CU-09.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Solicitar cancelación por arrendatario. | Datos de las precondiciones | Según el plan, 503 HARBOR_TIMEZONE_UNAVAILABLE; no se calcula clasificación ni cambia estado. | Pendiente | Contrato futuro |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | Sin publicación real |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
Probar también hora pactada incompleta; spec exige rechazo descriptivo, sin fijar aquí código.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Registrar los resultados cuando el flujo esté disponible; conservar identificadores para la inspección.

#### Traceability
FR-005, FR-006 | SC-004 | Sin prueba de CU-04 | Blocked
