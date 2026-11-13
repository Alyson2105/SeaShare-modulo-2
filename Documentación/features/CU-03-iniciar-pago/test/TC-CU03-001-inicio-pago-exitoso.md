### Test Case Specification: TC-CU03-001 — Inicio exitoso
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar inicio exitoso. Requisitos: FR-002, FR-003, FR-004, FR-005, FR-006. Criterios relacionados: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No se realiza publicación real ni cobro externo en esta tarea. La máquina de estados y el motor prueban transiciones internas, no la orquestación de pago.

##### Dependencies & Assumptions
Entorno futuro con CU-03 implementado, repositorio de reservas y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java); no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Una reserva INITIATED vigente y de su titular; M3 devuelve cálculo oficial y M1 disponibilidad.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | No ejecutado |
| 2 | Ejecutar | Enviar solicitud válida según el plan al endpoint de pagos. | Datos de las precondiciones | 200 OK, PENDING_PAYMENT, cálculo y desglose literales de M3; transición vía CU-08 y retención interna. | Pendiente | Integración: PaymentIntegrationTest#startPaymentTransitionsAndReturnsBreakdown pasó: 200, PENDING_PAYMENT y desglose simulado. No demuestra el proveedor real ni todos los efectos de retención. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La máquina de estados y el motor prueban transiciones internas, no la orquestación de pago.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Cobertura parcial; consultar el flujo para las comprobaciones pendientes.

#### Traceability
FR-002, FR-003, FR-004, FR-005, FR-006 | SC-001, SC-003 | [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); [maquina](../../../../src/test/java/com/seashare/m2/reservas/domain/state/ReservationStateMachineTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#startPaymentTransitionsAndReturnsBreakdown | In Process
