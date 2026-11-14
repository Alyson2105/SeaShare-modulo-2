### Test Case Specification: TC-CU04-005 — Fallo de zona horaria
**Status**: In Process
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
FR-005 exige obtener la zona por CU-09; el servicio usa la zona guardada y rechaza zona ausente o inválida. No se demuestra el fallo HTTP ni todas las entradas obligatorias.

##### Dependencies & Assumptions
Se requieren dependencias controladas; la evidencia descrita no acredita el flujo completo.

##### Preconditions & Default Setup
Reserva RESERVED; simular fallo o ausencia de la zona horaria desde CU-09.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar reserva, reloj y efectos iniciales. | Precondiciones | Entorno aislado disponible. | Pendiente | RequestCancellationServiceTest#nullHarborTimezoneIsUnavailable (XML V4: passed). Comprueba HARBOR_TIMEZONE_UNAVAILABLE ante zona nula con repositorio simulado; no demuestra consulta a CU-09 ni respuesta HTTP. |
| 2 | Ejecutar | Solicitar cancelación por arrendatario. | Datos de las precondiciones | Según el plan, 503 HARBOR_TIMEZONE_UNAVAILABLE; no se calcula clasificación ni cambia estado. | Pendiente | RequestCancellationServiceTest#nullHarborTimezoneIsUnavailable (XML V4: passed). Comprueba HARBOR_TIMEZONE_UNAVAILABLE ante zona nula con repositorio simulado; no demuestra consulta a CU-09 ni respuesta HTTP. |
| 3 | Comprobar | Inspeccionar respuesta y persistencia correspondientes. | Reserva y mensajes del escenario | Se cumplen los resultados del paso 2. | Pendiente | RequestCancellationServiceTest#nullHarborTimezoneIsUnavailable (XML V4: passed). Comprueba HARBOR_TIMEZONE_UNAVAILABLE ante zona nula con repositorio simulado; no demuestra consulta a CU-09 ni respuesta HTTP. |

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
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: RequestCancellationServiceTest#nullHarborTimezoneIsUnavailable. FR-005 exige obtener la zona por CU-09; el servicio usa la zona guardada y rechaza zona ausente o inválida. No se demuestra el fallo HTTP ni todas las entradas obligatorias.

#### Traceability
FR-005, FR-006 | SC-004 | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/RequestCancellationServiceTest.java#nullHarborTimezoneIsUnavailable | In Process
