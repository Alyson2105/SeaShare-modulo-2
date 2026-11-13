### Test Case Specification: TC-CU21-006 — Desglose financiero oficial
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU21-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar desglose financiero oficial. Requisitos: FR-003, FR-004, FR-010. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La identidad actual usa headers X-User-Id y X-User-Role, sin Spring Security. CU-03 asocia y persiste el cálculo; la integración probada simula M3, por lo que no acredita su servicio real.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: PaymentIntegrationTest#breakdownIsPersistedForDetailView; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva con liquidación oficial de M3 supuestamente asociada al pago.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar detalle y verificar rubros y total. | Datos de las precondiciones | El spec exige desglose de alquiler, seguro y depósito literal de M3. | Pendiente | PaymentIntegrationTest#breakdownIsPersistedForDetailView pasó: tras pago comprueba alquiler, seguro, depósito y total en GET con cálculo simulado. No acredita M3 real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-03 asocia y persiste el cálculo; la integración probada simula M3, por lo que no acredita su servicio real.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; prueba unitaria parcial con datos introducidos en memoria. PaymentIntegrationTest#breakdownIsPersistedForDetailView pasó: tras pago comprueba alquiler, seguro, depósito y total en GET con cálculo simulado. No acredita M3 real.

#### Traceability
FR-003, FR-004, FR-010 | SC-001 | Pruebas parciales ejecutadas  | src/test/java/com/seashare/m2/reservas/application/service/ReservationDetailPolicyTest.java#reservedShowsBreakdownAndUsesLiquidatedTotal; src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#breakdownIsPersistedForDetailView | In Process
