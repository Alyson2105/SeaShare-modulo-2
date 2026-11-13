### Test Case Specification: TC-CU08-004 — Expiración al límite
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU08-004

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar expiración al límite. Requisitos: FR-002, FR-003, FR-005. Criterios: SC-001, SC-003.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-08 es un puerto interno, sin endpoint HTTP. Probar un segundo antes conserva el estado; expiración es del motor y scheduler.

##### Dependencies & Assumptions
Ejecución con dependencias controladas cuando corresponda. Pruebas relacionadas: [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reservas INITIATED y PENDING_PAYMENT con vencimiento conocido y reloj controlado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Ejecutar expiración exactamente en ttl_expires_at. | Datos de las precondiciones | Cada reserva pasa a EXPIRED; no se encolan notificaciones M1 ni M3 para esa transición. | Pendiente | Integración: TtlExpiryIntegrationTest#expiresPendingPaymentPastTtl y #pendingPaymentBeforeTtlIsNotExpired pasaron para PENDING_PAYMENT y ausencia de evento de estado. Falta demostrar ambos estados iniciales y ausencia de M1 en el mismo caso. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Probar un segundo antes conserva el estado; expiración es del motor y scheduler.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Evidencia parcial; consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-002, FR-003, FR-005 | SC-001, SC-003 | [ttl](../../../../src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java#expiresPendingPaymentPastTtl; src/test/java/com/seashare/m2/reservas/integration/TtlExpiryIntegrationTest.java#pendingPaymentBeforeTtlIsNotExpired | In Process
