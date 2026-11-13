### Test Case Specification: TC-CU12-004 — Fallo del servicio financiero
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU12-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar fallo del servicio financiero. Requisitos: FR-007, FR-008. Criterios: SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-12 es integración saliente; M3 se simula y el modo fake es el predeterminado. CU-03 está implementado; falta verificar persistencia y ausencia de bloqueo M1 en el flujo de error integrado.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 simulado devuelve 422, 503 o error de conexión.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar cálculo para cada fallo. | Datos de las precondiciones | El adaptador lanza CALCULATION_UNAVAILABLE; ante ConnectException admite un reintento. | Pendiente | Componente WireMock: HttpCalculatedValueAdapterTest#serviceUnavailable503ThrowsCalculationUnavailable y #readTimeoutThrowsCalculationUnavailableWithoutRetry pasaron; unitario: StartPaymentServiceTest#calculationFailureAbortsWithoutTransition pasó con escritor no invocado. Falta comprobar reserva y M1 persistidos en integración. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-03 está implementado; falta verificar persistencia y ausencia de bloqueo M1 en el flujo de error integrado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial; evidencia parcial, consultar el flujo.

#### Traceability
FR-007, FR-008 | SC-004 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#serviceUnavailable503ThrowsCalculationUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#readTimeoutThrowsCalculationUnavailableWithoutRetry; src/test/java/com/seashare/m2/reservas/application/service/StartPaymentServiceTest.java#calculationFailureAbortsWithoutTransition | In Process
