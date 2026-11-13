### Test Case Specification: TC-CU15-001 — Consulta de reserva activa
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU15-001

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar consulta de reserva activa. Requisitos: FR-001, FR-002, FR-007. Criterios: SC-001, SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-15 expone un endpoint interno con identidad temporal por headers; JWT de servicio está pendiente. El endpoint interno CU-15 existe; sus pruebas usan vista simulada para HTTP.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: Sin prueba de CU-15; no demuestran el flujo completo de CU-15.

##### Preconditions & Default Setup
Reserva RESERVED o IN_NAVIGATION y servicio M3 autorizado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Consultar GET interno por reservation_id y comparar estado previo y posterior. | Datos de las precondiciones | El plan prevé 200 con identificadores, fechas, pasajeros y estado; sin escritura ni notificaciones. | Pendiente | Componente MockMvc: InternalReservationControllerTest#responseHasExactFieldsAndNoMoney pasó con vista simulada: 200, estructura y ausencia de montos. Falta comprobar escritura y notificaciones con persistencia real. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El endpoint interno CU-15 existe; sus pruebas usan vista simulada para HTTP.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Ejecutado el 2026-11-13: Partial; evidencia parcial, consultar el flujo.

#### Traceability
FR-001, FR-002, FR-007 | SC-001, SC-002 | Prueba parcial ejecutada | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java#responseHasExactFieldsAndNoMoney | In Process
