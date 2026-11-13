### Test Case Specification: TC-CU03-004 — Conservación del TTL
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU03-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar conservación del ttl. Requisitos: FR-006, FR-007. Criterios relacionados: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
No se realiza publicación real ni cobro externo en esta tarea. La expiración de PENDING_PAYMENT pertenece al motor CU-08; su test no prueba este endpoint.

##### Dependencies & Assumptions
CU-03 está implementado; la evidencia usa repositorio y dobles controlados de M1 y M3. Evidencia parcial de componentes internos: [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestra el flujo completo de CU-03.

##### Preconditions & Default Setup
Reserva INITIATED con ttl_expires_at conocido y reloj controlado antes del vencimiento.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Configurar precondiciones y registrar estado, TTL y outbox iniciales. | Datos aislados | Condiciones preparadas para la prueba futura. | Pendiente | No ejecutado |
| 2 | Ejecutar | Iniciar pago y comparar vencimiento persistido y devuelto con el valor previo. | Datos de las precondiciones | PENDING_PAYMENT conserva el mismo ttl_expires_at; ttl_remaining_seconds refleja el tiempo restante según contrato. | Pendiente | Integración: PaymentIntegrationTest#ttlIsNotRestarted pasó: vencimiento devuelto igual al original. Falta comparar vencimiento persistido y valor exacto de segundos restantes. |
| 3 | Inspeccionar | Comparar respuesta y efectos persistidos con lo esperado. | Reserva y eventos del escenario | Resultado coherente con el paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica otro comportamiento principal; los límites se indican abajo.

##### Edge Cases & Boundary Conditions
La expiración de PENDING_PAYMENT pertenece al motor CU-08; su test no prueba este endpoint.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial. Cobertura parcial; consultar el flujo para las comprobaciones pendientes.

#### Traceability
FR-006, FR-007 | SC-001 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); [estado](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/PaymentIntegrationTest.java#ttlIsNotRestarted | In Process
