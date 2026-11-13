### Test Case Specification: TC-CU06-001 — Inicio de navegación
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar inicio de navegación. Requisitos: FR-001, FR-003, FR-005, FR-006. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Verifica HTTP y check-in persistido; no verifica conservación de hora pactada ni entrega externa. El endpoint y servicio existen. Llegada tardía después de 30 minutos también es admisible mientras siga RESERVED.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java); no demuestran el flujo completo de CU-06.

##### Preconditions & Default Setup
Reserva RESERVED del propietario y reloj controlado en la hora exacta del zarpe.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent: verde. Verifica HTTP y check-in persistido; no verifica conservación de hora pactada ni entrega externa. |
| 2 | Ejecutar | Solicitar inicio con hora real y notas válidas. | Datos de las precondiciones | Según el plan, 200 e IN_NAVIGATION; se registra check-in y hora real, conservando la hora contratada. | Pendiente | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent: verde. Verifica HTTP y check-in persistido; no verifica conservación de hora pactada ni entrega externa. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationStartIntegrationTest#successfulStartPersistsDepartureStateAndCheckinEvent: verde. Verifica HTTP y check-in persistido; no verifica conservación de hora pactada ni entrega externa. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
Llegada tardía después de 30 minutos también es admisible mientras siga RESERVED.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001, FR-003, FR-005, FR-006 | SC-002 | [motor](../../../../src/test/java/com/seashare/m2/reservas/integration/ChangeReservationStatusServiceTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationStartIntegrationTest.java#successfulStartPersistsDepartureStateAndCheckinEvent | In Process
