### Test Case Specification: TC-CU03-003 — Contienda FCFS
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar contienda fcfs. Requisitos: FR-005, FR-008. Criterios relacionados: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Las solicitudes son secuenciales; no comprueba la carrera simultánea ni el estado persistido de la perdedora.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Dos titulares con reservas INITIATED vigentes para el mismo barco y fechas superpuestas; cálculo y flota disponibles.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | PaymentIntegrationTest#overlappingReservationLosesTheFcfsRace (XML V4: passed). Aserta 200 del primer pago y 409 CONCURRENCY_CONFLICT del segundo. Las solicitudes son secuenciales; no comprueba la carrera simultánea ni el estado persistido de la perdedora. |
| 2 | Ejecutar | Lanzar intentos de pago concurrentes coordinados, y consultar ambas reservas. | Datos de las precondiciones | Una transición a PENDING_PAYMENT como máximo; la perdedora permanece INITIATED y recibe 409 CONCURRENCY_CONFLICT según el plan. | Pendiente | PaymentIntegrationTest#overlappingReservationLosesTheFcfsRace (XML V4: passed). Aserta 200 del primer pago y 409 CONCURRENCY_CONFLICT del segundo. Las solicitudes son secuenciales; no comprueba la carrera simultánea ni el estado persistido de la perdedora. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | PaymentIntegrationTest#overlappingReservationLosesTheFcfsRace (XML V4: passed). Aserta 200 del primer pago y 409 CONCURRENCY_CONFLICT del segundo. Las solicitudes son secuenciales; no comprueba la carrera simultánea ni el estado persistido de la perdedora. |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La restricción GiST y pruebas del motor no demuestran por sí solas la orquestación FCFS de CU-03.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: PaymentIntegrationTest#overlappingReservationLosesTheFcfsRace. Las solicitudes son secuenciales; no comprueba la carrera simultánea ni el estado persistido de la perdedora.

#### Traceability
FR-005, FR-008 | SC-002 | [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#overlappingReservationLosesTheFcfsRace | In Process
