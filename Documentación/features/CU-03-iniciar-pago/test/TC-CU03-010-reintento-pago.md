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
El spec FR-002 limita el inicio a INITIATED; el plan, Notas Transversales «Reintento de pago con otra tarjeta», permite reintento desde PENDING_PAYMENT tras REJECTED.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Las pruebas citadas aportan evidencia parcial, no demuestran el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva PENDING_PAYMENT con cobro REJECTED y TTL vigente, conforme a la nota del plan.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | StartPaymentServiceTest#retryAfterRejectedReusesThePersistedCalculation (XML V4: passed). Aserta attempt 2 y verifica que no se vuelve a consultar el cálculo. El spec FR-002 limita el inicio a INITIATED; el plan, Notas Transversales «Reintento de pago con otra tarjeta», permite reintento desde PENDING_PAYMENT tras REJECTED. |
| 2 | Ejecutar | Intentar nuevamente con token distinto. | Datos de las precondiciones | El plan propone reutilizar cálculo y TTL, incrementar attempt y publicar nuevo evento; requiere resolver la contradicción con FR-002. | Pendiente | StartPaymentServiceTest#retryAfterRejectedReusesThePersistedCalculation (XML V4: passed). Aserta attempt 2 y verifica que no se vuelve a consultar el cálculo. El spec FR-002 limita el inicio a INITIATED; el plan, Notas Transversales «Reintento de pago con otra tarjeta», permite reintento desde PENDING_PAYMENT tras REJECTED. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | StartPaymentServiceTest#retryAfterRejectedReusesThePersistedCalculation (XML V4: passed). Aserta attempt 2 y verifica que no se vuelve a consultar el cálculo. El spec FR-002 limita el inicio a INITIATED; el plan, Notas Transversales «Reintento de pago con otra tarjeta», permite reintento desde PENDING_PAYMENT tras REJECTED. |

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
Corrida V4 del 2026-11-13: Draft; estado documental: Draft; evidencia: StartPaymentServiceTest#retryAfterRejectedReusesThePersistedCalculation. El spec FR-002 limita el inicio a INITIATED; el plan, Notas Transversales «Reintento de pago con otra tarjeta», permite reintento desde PENDING_PAYMENT tras REJECTED.

#### Traceability
FR-002, FR-009 | SC-001, SC-003 | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#retryAfterRejectedReusesThePersistedCalculation | Draft
