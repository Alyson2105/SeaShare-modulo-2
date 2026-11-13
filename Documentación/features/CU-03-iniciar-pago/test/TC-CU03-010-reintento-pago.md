### Test Case Specification: TC-CU03-010 — Reintento de pago rechazado
**Status**: Draft
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-010

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reintento de pago rechazado. Requisitos: FR-002, FR-009. Criterios relacionados: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No se realiza publicación real ni cobro externo en esta tarea. Spec limita el inicio a INITIATED, pero el plan permite reintento desde PENDING_PAYMENT; confirmar contrato y clave de idempotencia con M3.

##### Dependencies & Assumptions
Entorno futuro con CU-03 implementado, repositorio de reservas y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: Sin prueba de CU-03; no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva PENDING_PAYMENT con cobro REJECTED y TTL vigente, conforme a la nota del plan.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | No ejecutado |
| 2 | Ejecutar | Intentar nuevamente con token distinto. | Datos de las precondiciones | El plan propone reutilizar cálculo y TTL, incrementar attempt y publicar nuevo evento; requiere resolver la contradicción con FR-002. | Pendiente | Contrato contradictorio: spec limita a INITIATED y plan permite reintento tras REJECTED. StartPaymentServiceTest#retryAfterRejectedReusesThePersistedCalculation pasó: intento 2 y reutilización; no resuelve el contrato ni verifica el evento. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
Spec limita el inicio a INITIATED, pero el plan permite reintento desde PENDING_PAYMENT; confirmar contrato y clave de idempotencia con M3.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Draft. Contrato pendiente de aclaración.

#### Traceability
FR-002, FR-009 | SC-001, SC-003 | Sin prueba de CU-03 | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#retryAfterRejectedReusesThePersistedCalculation | Draft
