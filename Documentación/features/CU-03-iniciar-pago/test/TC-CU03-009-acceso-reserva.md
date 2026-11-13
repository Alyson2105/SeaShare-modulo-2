### Test Case Specification: TC-CU03-009 — Autorización del titular
**Status**: Blocked
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-009

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar autorización del titular. Requisitos: FR-001, FR-002. Criterios relacionados: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-03 y su endpoint aún no están implementados. Los resultados siguientes son expectativas del plan, no comportamiento observado. No se realiza publicación real ni cobro externo en esta tarea. La autenticación Bearer descrita por el plan no está implementada; no se presenta como header ejecutable actual.

##### Dependencies & Assumptions
Entorno futuro con CU-03 implementado, repositorio de reservas y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: Sin prueba de CU-03; no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva INITIATED vigente perteneciente a un arrendatario; usar otro usuario autenticado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | No ejecutado |
| 2 | Ejecutar | Intentar iniciar pago de la reserva ajena. | Datos de las precondiciones | Según el plan, 403 RESERVATION_NOT_OWNED sin transición. | Pendiente | Contrato pendiente de implementación |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La autenticación Bearer descrita por el plan no está implementada; no se presenta como header ejecutable actual.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Registrar resultado cuando se habilite la ejecución; conservar identificadores de reserva y eventos de prueba.

#### Traceability
FR-001, FR-002 | SC-001 | Sin prueba de CU-03 | Blocked
