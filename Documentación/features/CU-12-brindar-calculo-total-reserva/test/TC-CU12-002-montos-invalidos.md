### Test Case Specification: TC-CU12-002 — Desglose inválido
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU12-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar desglose inválido. Requisitos: FR-005, FR-007. Criterios: SC-002, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-12 es integración saliente; M3 se simula y el modo fake es el predeterminado. CU-03 está implementado; la ausencia de transición se prueba a nivel unitario con escritor simulado.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
M3 responde importes ausentes, no numéricos o negativos.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar variantes con cada campo inválido. | Datos de las precondiciones | El adaptador lanza CALCULATION_UNAVAILABLE; alquiler y total cero se rechazan, seguro y depósito cero se aceptan. | Pendiente | Componente WireMock: HttpCalculatedValueAdapterTest#missingFieldThrowsCalculationUnavailable, #zeroTotalThrowsCalculationUnavailable, #zeroRentalThrowsCalculationUnavailable y #zeroInsuranceAndDepositAreAccepted pasaron. No cubren todas las variantes ni el flujo persistido. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-03 está implementado; la ausencia de transición se prueba a nivel unitario con escritor simulado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial; evidencia parcial, consultar el flujo.

#### Traceability
FR-005, FR-007 | SC-002, SC-003 | [adaptador](../../../../src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#missingFieldThrowsCalculationUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#zeroTotalThrowsCalculationUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#zeroRentalThrowsCalculationUnavailable; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/out/finance/HttpCalculatedValueAdapterTest.java#zeroInsuranceAndDepositAreAccepted | In Process
