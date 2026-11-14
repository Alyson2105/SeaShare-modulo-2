### Test Case Specification: TC-CU08-002 — Rechazo de transición inválida
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo de transición inválida. Requisitos: FR-002, FR-003, FR-005. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
La persistencia sin mutación para cada salida terminal no se demuestra; cancelFromPendingPaymentIsRejected cubre otra transición con espías sobre los puertos.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas en estados terminales o PENDING_PAYMENT.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | ReservationStateMachineTest#finalStatesHaveNoExit (XML V4: passed). Comprueba que los estados terminales no tienen transiciones salientes en la máquina de estados. La persistencia sin mutación para cada salida terminal no se demuestra; cancelFromPendingPaymentIsRejected cubre otra transición con espías sobre los puertos. |
| 2 | Ejecutar | Intentar salidas desde COMPLETED, CANCELLED, EXPIRED y PAYMENT_FAILED y cancelación desde PENDING_PAYMENT. | Datos de las precondiciones | El motor rechaza cada transición sin modificar estado. | Pendiente | ReservationStateMachineTest#finalStatesHaveNoExit (XML V4: passed). Comprueba que los estados terminales no tienen transiciones salientes en la máquina de estados. La persistencia sin mutación para cada salida terminal no se demuestra; cancelFromPendingPaymentIsRejected cubre otra transición con espías sobre los puertos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | ReservationStateMachineTest#finalStatesHaveNoExit (XML V4: passed). Comprueba que los estados terminales no tienen transiciones salientes en la máquina de estados. La persistencia sin mutación para cada salida terminal no se demuestra; cancelFromPendingPaymentIsRejected cubre otra transición con espías sobre los puertos. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El conflicto de disponibilidad FCFS es CU-03, no este caso.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: ReservationStateMachineTest#finalStatesHaveNoExit. La persistencia sin mutación para cada salida terminal no se demuestra; cancelFromPendingPaymentIsRejected cubre otra transición con espías sobre los puertos.

#### Traceability
FR-002, FR-003, FR-005 | SC-001 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java#finalStatesHaveNoExit; src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java#cancelFromPendingPaymentIsRejected | In Process
