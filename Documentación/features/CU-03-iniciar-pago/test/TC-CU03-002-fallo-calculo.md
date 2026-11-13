### Test Case Specification: TC-CU03-002 — Fallo de cálculo externo
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo de cálculo externo. Requisitos: FR-003, FR-006. Criterios relacionados: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No se realiza publicación real ni cobro externo en esta tarea. La respuesta 503 requiere comprobación HTTP en el escenario.

##### Dependencies & Assumptions
Entorno futuro con CU-03 implementado, repositorio de reservas y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: Sin prueba de CU-03; no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva INITIATED vigente; simular indisponibilidad del cálculo de M3.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | No ejecutado |
| 2 | Ejecutar | Intentar iniciar pago. | Datos de las precondiciones | Según el plan, 503 CALCULATION_UNAVAILABLE; reserva permanece INITIATED con TTL original, sin retención ni notificación M1. | Pendiente | Unitario: StartPaymentServiceTest#calculationFailureAbortsWithoutTransition pasó: CALCULATION_UNAVAILABLE y escritor no invocado. Faltan HTTP 503 y comprobación de TTL, outbox y M1 persistidos. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La respuesta 503 requiere comprobación HTTP en el escenario.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Cobertura parcial; consultar el flujo para las comprobaciones pendientes.

#### Traceability
FR-003, FR-006 | SC-001, SC-003 | Sin prueba de CU-03 | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#calculationFailureAbortsWithoutTransition | In Process
