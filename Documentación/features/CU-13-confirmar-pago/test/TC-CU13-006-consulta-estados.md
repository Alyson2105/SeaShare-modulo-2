### Test Case Specification: TC-CU13-006 — Consulta del resultado en M3
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU13-006

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consulta del resultado en m3. Requisitos: FR-001, FR-003, FR-005. Criterios: SC-001, SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Plan externo | Contrato de consulta | [plan.external](../plan.external.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
HttpPaymentStatusAdapterTest#notFoundMeansRetryNextCycle demuestra UNKNOWN por separado; no comprueba ciclos sucesivos completos ni persistencia.

##### Dependencies & Assumptions
Ejecución V4 con dependencias controladas. Las pruebas citadas son parciales y no demuestran el flujo completo del caso.

##### Preconditions & Default Setup
Reserva PENDING_PAYMENT vigente y M3 simulado responde IN_PROCESS, UNKNOWN o error.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | PollPaymentStatusServiceTest#inProcessIsSkipped (XML V4: passed). Aserta cero procesadas y verifica que no se invoque confirmación, con dependencias simuladas. HttpPaymentStatusAdapterTest#notFoundMeansRetryNextCycle demuestra UNKNOWN por separado; no comprueba ciclos sucesivos completos ni persistencia. |
| 2 | Ejecutar | Ejecutar ciclos del canal B. | Datos de las precondiciones | El plan prevé no cambiar el estado y consultar en siguiente ciclo sin asumir aprobación. | Pendiente | PollPaymentStatusServiceTest#inProcessIsSkipped (XML V4: passed). Aserta cero procesadas y verifica que no se invoque confirmación, con dependencias simuladas. HttpPaymentStatusAdapterTest#notFoundMeansRetryNextCycle demuestra UNKNOWN por separado; no comprueba ciclos sucesivos completos ni persistencia. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | PollPaymentStatusServiceTest#inProcessIsSkipped (XML V4: passed). Aserta cero procesadas y verifica que no se invoque confirmación, con dependencias simuladas. HttpPaymentStatusAdapterTest#notFoundMeansRetryNextCycle demuestra UNKNOWN por separado; no comprueba ciclos sucesivos completos ni persistencia. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El job de consulta y la lógica del canal B están implementados.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: PollPaymentStatusServiceTest#inProcessIsSkipped. HttpPaymentStatusAdapterTest#notFoundMeansRetryNextCycle demuestra UNKNOWN por separado; no comprueba ciclos sucesivos completos ni persistencia.

#### Traceability
FR-001, FR-003, FR-005 | SC-001, SC-002 | Lógica de confirmación probada | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/PollPaymentStatusServiceTest.java#inProcessIsSkipped; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpPaymentStatusAdapterTest.java#notFoundMeansRetryNextCycle | In Process
