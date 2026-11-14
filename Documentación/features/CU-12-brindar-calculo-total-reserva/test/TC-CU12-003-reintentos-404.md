### Test Case Specification: TC-CU12-003 — Información aún no disponible
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU12-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar información aún no disponible. Requisitos: FR-003, FR-004, FR-008. Criterios: SC-003, SC-004.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Plan.md §5 y §6 prevé reintentos de 404; el caso exige reintentar. La prueba confirma que el adaptador falla inmediatamente sin reintentar, aunque la aserción Java sea exitosa.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 simulado responde 404 y luego 200 o mantiene 404; reloj y retrasos controlados.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | HttpCalculatedValueAdapterTest#notFoundFailsImmediatelyWithoutRetry (XML V4: passed). Aserta CALCULATION_UNAVAILABLE y verifica exactamente un POST ante 404 persistente. Plan.md §5 y §6 prevé reintentos de 404; el caso exige reintentar. La prueba confirma que el adaptador falla inmediatamente sin reintentar, aunque la aserción Java sea exitosa. |
| 2 | Ejecutar | Solicitar cálculo y contar intentos y presupuesto. | Datos de las precondiciones | El adaptador reintenta 404 hasta el límite configurado y presupuesto; si persiste devuelve CALCULATION_UNAVAILABLE. | Falló | HttpCalculatedValueAdapterTest#notFoundFailsImmediatelyWithoutRetry (XML V4: passed). Aserta CALCULATION_UNAVAILABLE y verifica exactamente un POST ante 404 persistente. Plan.md §5 y §6 prevé reintentos de 404; el caso exige reintentar. La prueba confirma que el adaptador falla inmediatamente sin reintentar, aunque la aserción Java sea exitosa. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | HttpCalculatedValueAdapterTest#notFoundFailsImmediatelyWithoutRetry (XML V4: passed). Aserta CALCULATION_UNAVAILABLE y verifica exactamente un POST ante 404 persistente. Plan.md §5 y §6 prevé reintentos de 404; el caso exige reintentar. La prueba confirma que el adaptador falla inmediatamente sin reintentar, aunque la aserción Java sea exitosa. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El plan prevé reintentos 404; el adaptador actual responde de inmediato sin reintentar.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Failed; estado documental: In Process; evidencia: HttpCalculatedValueAdapterTest#notFoundFailsImmediatelyWithoutRetry. Plan.md §5 y §6 prevé reintentos de 404; el caso exige reintentar. La prueba confirma que el adaptador falla inmediatamente sin reintentar, aunque la aserción Java sea exitosa.

#### Traceability
FR-003, FR-004, FR-008 | SC-003, SC-004 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#notFoundFailsImmediatelyWithoutRetry; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#persistentNotFoundThrowsCalculationUnavailable | In Process

#### Evidencia local posterior a V4 — 2026-11-13
HttpCalculatedValueAdapterTest#notFoundRetriesUntilCalculationIsAvailable y #persistentNotFoundThrowsCalculationUnavailableAfterThreeRetries: respuestas HTTP simuladas, no integración real con M3; ambas pasaron en la suite completa de 640 pruebas del 2026-11-13. Veredicto técnico: Partial; no se declara integración externa exitosa.
