### Test Case Specification: TC-CU03-005 — Consentimiento obligatorio
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consentimiento obligatorio. Requisitos: FR-013, FR-014. Criterios relacionados: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No cubre aceptación ausente ni comprueba ausencia de transición y envío para ambas variantes.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Las pruebas citadas aportan evidencia parcial, no demuestran el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva INITIATED válida; preparar solicitudes independientes con accepts_cancellation_policy false y ausente.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | PaymentIntegrationTest#policyNotAcceptedIs400 (XML V4: passed). Aserta HTTP 400 POLICY_NOT_ACCEPTED con consentimiento falso. No cubre aceptación ausente ni comprueba ausencia de transición y envío para ambas variantes. |
| 2 | Ejecutar | Enviar cada variante al endpoint y revisar efectos. | Datos de las precondiciones | Según el plan, 400 POLICY_NOT_ACCEPTED; sin transición ni envío a M3. | Pendiente | PaymentIntegrationTest#policyNotAcceptedIs400 (XML V4: passed). Aserta HTTP 400 POLICY_NOT_ACCEPTED con consentimiento falso. No cubre aceptación ausente ni comprueba ausencia de transición y envío para ambas variantes. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | PaymentIntegrationTest#policyNotAcceptedIs400 (XML V4: passed). Aserta HTTP 400 POLICY_NOT_ACCEPTED con consentimiento falso. No cubre aceptación ausente ni comprueba ausencia de transición y envío para ambas variantes. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La selección visual del checkbox es responsabilidad de UI y se prueba por separado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: PaymentIntegrationTest#policyNotAcceptedIs400. No cubre aceptación ausente ni comprueba ausencia de transición y envío para ambas variantes.

#### Traceability
FR-013, FR-014 | SC-004 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#policyNotAcceptedIs400 | In Process
