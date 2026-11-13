### Test Case Specification: TC-CU06-005 — Travesía anterior sin cerrar
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU06-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar travesía anterior sin cerrar. Requisitos: FR-001, FR-004. Criterios: SC-001.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Verifica dos reservas secuenciales y rechazo; no demuestra inicios simultáneos. El endpoint y servicio existen. La validación del endpoint depende de CU-06 y la finalización de CU-07.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-06.

##### Preconditions & Default Setup
Otra reserva de la misma embarcación permanece IN_NAVIGATION sin check-out.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NavigationStartIntegrationTest#anotherReservationOfSameVesselInNavigationIsRejected: verde. Verifica dos reservas secuenciales y rechazo; no demuestra inicios simultáneos. |
| 2 | Ejecutar | Solicitar inicio de la reserva RESERVED. | Datos de las precondiciones | Según el plan, 409 VESSEL_CURRENTLY_IN_NAVIGATION sin transición. | Pendiente | NavigationStartIntegrationTest#anotherReservationOfSameVesselInNavigationIsRejected: verde. Verifica dos reservas secuenciales y rechazo; no demuestra inicios simultáneos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationStartIntegrationTest#anotherReservationOfSameVesselInNavigationIsRejected: verde. Verifica dos reservas secuenciales y rechazo; no demuestra inicios simultáneos. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
La validación del endpoint depende de CU-06 y la finalización de CU-07.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-001, FR-004 | SC-001 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationStartIntegrationTest.java#anotherReservationOfSameVesselInNavigationIsRejected | In Process
