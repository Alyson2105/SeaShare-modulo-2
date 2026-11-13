### Test Case Specification: TC-CU03-006 — Datos obligatorios de pago
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar datos obligatorios de pago. Requisitos: FR-009, FR-014. Criterios relacionados: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No se realiza publicación real ni cobro externo en esta tarea. El token debe ser de prueba; no introducir tarjeta real ni CVV.

##### Dependencies & Assumptions
Entorno futuro con CU-03 implementado, repositorio de reservas y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: Sin prueba de CU-03; no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Consentimiento verdadero; preparar variantes independientes omitiendo token, payer_email, payment_method_id o installments.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | No ejecutado |
| 2 | Ejecutar | Enviar cada solicitud al endpoint y revisar estado y outbox. | Datos de las precondiciones | Según el plan, 400 PAYMENT_DATA_INCOMPLETE y no se inicia el cobro. | Pendiente | Integración: PaymentIntegrationTest#missingPaymentTokenIs400 pasó: 400 PAYMENT_DATA_INCOMPLETE. Faltan variantes y comprobación de estado y outbox. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
El token debe ser de prueba; no introducir tarjeta real ni CVV.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Cobertura parcial; consultar el flujo para las comprobaciones pendientes.

#### Traceability
FR-009, FR-014 | SC-004 | Sin prueba de CU-03 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#missingPaymentTokenIs400 | In Process
