### Test Case Specification: TC-CU08-009 — Creación por CU-08
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-009

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar creación por cu-08. Requisitos: FR-001, FR-002, FR-003. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La delegación a CU-08 se prueba separadamente con mocks; falta invocación directa con reloj controlado y comprobación del TTL persistido.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: CreateReservationTest y ChangeReservationStatusServiceTest; su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Solicitud de creación desde CU-02.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | CreateReservationTest#createsReservationInInitiated (XML V4: passed). Aserta estado INITIATED persistido y TTL de 15 minutos en respuesta. La delegación a CU-08 se prueba separadamente con mocks; falta invocación directa con reloj controlado y comprobación del TTL persistido. |
| 2 | Ejecutar | Invocar la creación por CU-08. | Datos de las precondiciones | Reserva nace INITIATED por el motor, con TTL de 15 minutos. | Pendiente | CreateReservationTest#createsReservationInInitiated (XML V4: passed). Aserta estado INITIATED persistido y TTL de 15 minutos en respuesta. La delegación a CU-08 se prueba separadamente con mocks; falta invocación directa con reloj controlado y comprobación del TTL persistido. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | CreateReservationTest#createsReservationInInitiated (XML V4: passed). Aserta estado INITIATED persistido y TTL de 15 minutos en respuesta. La delegación a CU-08 se prueba separadamente con mocks; falta invocación directa con reloj controlado y comprobación del TTL persistido. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
CU-02 delega la creación en CU-08; el caso requiere verificar directamente el motor con reloj controlado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: CreateReservationTest#createsReservationInInitiated. La delegación a CU-08 se prueba separadamente con mocks; falta invocación directa con reloj controlado y comprobación del TTL persistido.

#### Traceability
FR-001, FR-002, FR-003 | SC-001 | Pruebas de creación ejecutadas | Evidencia: src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java#createsReservationInInitiated; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#creationWritesAuditRow; src/test/java/com/seashare/m2/reservas/application/service/CreateReservationServiceTest.java#creationDelegatesToInitiateUseCase | In Process
