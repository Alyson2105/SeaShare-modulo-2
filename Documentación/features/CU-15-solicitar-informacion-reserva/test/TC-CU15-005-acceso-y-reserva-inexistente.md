### Test Case Specification: TC-CU15-005 — Autorización de consulta
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU15-005

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P1

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar autorización de consulta. Requisitos: FR-001, FR-007. Criterios: SC-002.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
CU-15 expone un endpoint interno con identidad temporal por headers; JWT de servicio está pendiente. El endpoint utiliza identidad temporal por headers; JWT de servicio está pendiente.

##### Dependencies & Assumptions
Ejecución futura con dependencias controladas. Pruebas parciales de componentes: Sin prueba de CU-15; no demuestran el flujo completo de CU-15.

##### Preconditions & Default Setup
Reserva existente y credenciales de servicio no autorizadas.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | No ejecutado |
| 2 | Ejecutar | Intentar consulta con identidad distinta de M3. | Datos de las precondiciones | El plan prevé 403 PROFILE_NOT_AUTHORIZED; no hay mutación. | Pendiente | Componente MockMvc: InternalReservationControllerTest#nonFinanceRoleIs403 y #unknownReservationIs404 pasaron con headers temporales y servicio simulado. No prueban JWT de servicio ni ausencia de escritura en base de datos. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | No ejecutado |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
El endpoint utiliza identidad temporal por headers; JWT de servicio está pendiente.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial, consultar el flujo.

#### Traceability
FR-001, FR-007 | SC-002 | Prueba parcial ejecutada | Evidencia: src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java#nonFinanceRoleIs403; src/test/java/com/seashare/m2/reservas/infrastructure/adapter/in/web/InternalReservationControllerTest.java#unknownReservationIs404 | In Process
