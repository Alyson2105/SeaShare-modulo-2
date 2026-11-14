### Test Case Specification: TC-CU12-005 — Asociación al pago
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU12-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar asociación al pago. Requisitos: FR-001, FR-006, FR-009. Criterios: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No verifica M3 real ni recepción del evento previo de reserva; no demuestra ausencia global de aritmética.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: PaymentIntegrationTest y StartPaymentServiceTest con cálculo simulado; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva INITIATED y cálculo oficial de M3.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | PaymentIntegrationTest#breakdownIsPersistedForDetailView (XML V4: passed). Tras iniciar pago, aserta en detalle alquiler, seguro, depósito y total proveniente de cálculo simulado. No verifica M3 real ni recepción del evento previo de reserva; no demuestra ausencia global de aritmética. |
| 2 | Ejecutar | Iniciar pago y revisar transición y montos asociados. | Datos de las precondiciones | El spec exige asociar valores sin alteración antes de PENDING_PAYMENT. | Pendiente | PaymentIntegrationTest#breakdownIsPersistedForDetailView (XML V4: passed). Tras iniciar pago, aserta en detalle alquiler, seguro, depósito y total proveniente de cálculo simulado. No verifica M3 real ni recepción del evento previo de reserva; no demuestra ausencia global de aritmética. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | PaymentIntegrationTest#breakdownIsPersistedForDetailView (XML V4: passed). Tras iniciar pago, aserta en detalle alquiler, seguro, depósito y total proveniente de cálculo simulado. No verifica M3 real ni recepción del evento previo de reserva; no demuestra ausencia global de aritmética. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-03 invoca el puerto y asocia los montos; las pruebas usan cálculo simulado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: PaymentIntegrationTest#breakdownIsPersistedForDetailView. No verifica M3 real ni recepción del evento previo de reserva; no demuestra ausencia global de aritmética.

#### Traceability
FR-001, FR-006, FR-009 | SC-001, SC-003 | Pruebas de pago ejecutadas | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#startPaymentTransitionsAndReturnsBreakdown; src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#breakdownIsPersistedForDetailView; src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#validPaymentDelegatesToTransactionalWriter | In Process
