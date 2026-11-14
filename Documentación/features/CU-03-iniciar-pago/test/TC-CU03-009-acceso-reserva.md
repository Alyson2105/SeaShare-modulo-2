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
El plan, Elementos de la Petición, exige Bearer JWT y el setup exige usuario autenticado; el resolver actual solo lee cabeceras de identidad. Falta además comprobar que no haya transición persistida.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Las pruebas citadas aportan evidencia parcial, no demuestran el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva INITIATED vigente perteneciente a un arrendatario; usar otro usuario autenticado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | PaymentIntegrationTest#ownerCannotPay (XML V4: passed). Aserta HTTP 403 RESERVATION_NOT_OWNED usando X-User-Id y X-User-Role de un usuario ajeno. El plan, Elementos de la Petición, exige Bearer JWT y el setup exige usuario autenticado; el resolver actual solo lee cabeceras de identidad. Falta además comprobar que no haya transición persistida. |
| 2 | Ejecutar | Intentar iniciar pago de la reserva ajena. | Datos de las precondiciones | Según el plan, 403 RESERVATION_NOT_OWNED sin transición. | Pendiente | PaymentIntegrationTest#ownerCannotPay (XML V4: passed). Aserta HTTP 403 RESERVATION_NOT_OWNED usando X-User-Id y X-User-Role de un usuario ajeno. El plan, Elementos de la Petición, exige Bearer JWT y el setup exige usuario autenticado; el resolver actual solo lee cabeceras de identidad. Falta además comprobar que no haya transición persistida. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | PaymentIntegrationTest#ownerCannotPay (XML V4: passed). Aserta HTTP 403 RESERVATION_NOT_OWNED usando X-User-Id y X-User-Role de un usuario ajeno. El plan, Elementos de la Petición, exige Bearer JWT y el setup exige usuario autenticado; el resolver actual solo lee cabeceras de identidad. Falta además comprobar que no haya transición persistida. |

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
Corrida V4 del 2026-11-13: Blocked; estado documental: Blocked; evidencia: PaymentIntegrationTest#ownerCannotPay. El plan, Elementos de la Petición, exige Bearer JWT y el setup exige usuario autenticado; el resolver actual solo lee cabeceras de identidad. Falta además comprobar que no haya transición persistida.

#### Traceability
FR-001, FR-002 | SC-001 | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#otherRenterIsForbidden; src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#ownerCannotPay | Blocked
