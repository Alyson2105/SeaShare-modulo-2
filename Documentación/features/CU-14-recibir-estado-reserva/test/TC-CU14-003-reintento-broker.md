### Test Case Specification: TC-CU14-003 — Reintentos del relay
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU14-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar reintentos del relay. Requisitos: FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La emisión se inicia desde CU-08 y usa outbox; no hay respuesta de M3. Broker confirm no demuestra procesamiento por el consumidor de M3.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Mensaje AMQP pendiente y publicador simulado sin confirmación.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Ejecutar intentos con reloj y jitter controlados. | Datos de las precondiciones | El relay reintenta con backoff; al quinto fallo marca dead y conserva error. | Pendiente | Unitario: PublishPendingOutboxServiceTest#backoffExactoUnoCincoVeinticincoCientoVeinticincoSegundos y #quintoFalloMarcaDeadConAlerta pasaron con reloj y jitter controlados; falta confirmar los cinco intentos con broker real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Broker confirm no demuestra procesamiento por el consumidor de M3.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-005 | SC-001 | [relay](../../../../src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java#backoffExactoUnoCincoVeinticincoCientoVeinticincoSegundos; src/test/java/com/seashare/m2/reservas/application/service/PublishPendingOutboxServiceTest.java#quintoFalloMarcaDeadConAlerta | In Process
