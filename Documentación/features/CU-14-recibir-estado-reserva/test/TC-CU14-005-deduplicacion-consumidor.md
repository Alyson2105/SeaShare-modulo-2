### Test Case Specification: TC-CU14-005 — Entrega duplicada
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU14-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar entrega duplicada. Requisitos: FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
El consumidor es de prueba; no demuestra deduplicación ni efectos persistidos del consumidor M3 real.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [deduplicacion](../../../../src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Dos entregas con el mismo Message-Id a consumidor de prueba.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | EventDeduplicationTest#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez (XML V4: passed). Aserta dos entregas al consumidor de prueba, un procesamiento y Message-Id igual al event_id. El consumidor es de prueba; no demuestra deduplicación ni efectos persistidos del consumidor M3 real. |
| 2 | Ejecutar | Procesar ambas copias y observar contador de procesamiento. | Datos de las precondiciones | El consumidor de prueba procesa una sola vez; cada copia conserva identificador. | Pasó | EventDeduplicationTest#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez (XML V4: passed). Aserta dos entregas al consumidor de prueba, un procesamiento y Message-Id igual al event_id. El consumidor es de prueba; no demuestra deduplicación ni efectos persistidos del consumidor M3 real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | EventDeduplicationTest#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez (XML V4: passed). Aserta dos entregas al consumidor de prueba, un procesamiento y Message-Id igual al event_id. El consumidor es de prueba; no demuestra deduplicación ni efectos persistidos del consumidor M3 real. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El consumidor real pertenece a M3 y no se demuestra su deduplicación.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: EventDeduplicationTest#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez. El consumidor es de prueba; no demuestra deduplicación ni efectos persistidos del consumidor M3 real.

#### Traceability
FR-005 | SC-001 | [deduplicacion](../../../../src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/EventDeduplicationTest.java#publicarElMismoEventoDosVecesSeProcesaUnaSolaVez | In Process
