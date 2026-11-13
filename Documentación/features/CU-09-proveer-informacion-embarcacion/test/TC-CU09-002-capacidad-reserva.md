### Test Case Specification: TC-CU09-002 — Rechazo por exceso de capacidad
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU09-002

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar rechazo por exceso de capacidad. Requisitos: FR-002, FR-005, FR-006. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-09 es integración saliente; M1 se simula y el modo fake es el predeterminado. FleetInfoPort no recibe pasajeros ni valida capacidad; la validación actual pertenece a CreateReservationService.

##### Dependencies & Assumptions
Ejecución con dependencias controladas. Pruebas relacionadas: [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java); su cobertura debe evaluarse por escenario.

##### Preconditions & Default Setup
Reserva nueva con pasajeros mayores que maxCapacity devuelta por flota.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Ejecutar creación CU-02 con dicha capacidad. | Datos de las precondiciones | CU-02 rechaza por CAPACITY_EXCEEDED y no crea reserva. | Pendiente | Integración: CreateReservationTest#rejectsPassengersOverCapacity pasó: 409 CAPACITY_EXCEEDED y cero reservas del arrendatario; flota simulada. La validación se realiza en CU-02. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
FleetInfoPort no recibe pasajeros ni valida capacidad; la validación actual pertenece a CreateReservationService.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial. Evidencia parcial; consultar las comprobaciones pendientes del flujo.

#### Traceability
FR-002, FR-005, FR-006 | SC-002 | [reserva](../../../../src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java) | Evidencia: src/test/java/com/seashare/m2/reservas/integration/CreateReservationTest.java#rejectsPassengersOverCapacity | In Process
