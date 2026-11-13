### Test Case Specification: TC-CU07-003 — Autorización del propietario
**Status**: In Process
**Created** : 2026-11-10  **Spec/Plan Link** : [Spec](../spec.md) · [Plan](../plan.md)

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-CU07-003

**Product Name & Version** : SeaShare M2 v0.1

**Author** : Juan Manuel

**Current Owner** : QA Lead

**Priority** : P2

#### Objectives & Context  *(mandatory)*
##### Test Objectives
Verificar autorización del propietario. Requisitos: FR-002. Criterios: SC-005.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Spec | Especificación | [spec](../spec.md) |
| Plan | Contrato propuesto | [plan](../plan.md) |
| Context | Estado de implementación | [implementContext](../../../context/implementContext.md) |

#### Prerequisites & Environment
##### Test Case Limitations
Verifica ambos 403; usa identidad por cabeceras, sin comprobar JWT ni efectos posteriores. El endpoint y servicio existen. JWT es propuesto y aún no está implementado.

##### Dependencies & Assumptions
Evidencia de la corrida con dependencias controladas. Pruebas parciales de componentes: La prueba citada cubre solo parte del escenario; no demuestra el flujo completo de CU-07.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION; solicitante distinto del propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pendiente | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden: verde. Verifica ambos 403; usa identidad por cabeceras, sin comprobar JWT ni efectos posteriores. |
| 2 | Ejecutar | Solicitar cierre. | Datos de las precondiciones | Según el plan, 403 PROFILE_NOT_AUTHORIZED; sin transición. | Pendiente | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden: verde. Verifica ambos 403; usa identidad por cabeceras, sin comprobar JWT ni efectos posteriores. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden: verde. Verifica ambos 403; usa identidad por cabeceras, sin comprobar JWT ni efectos posteriores. |

##### Section 2: Secondary Flow / Error Handling
No aplica comportamiento principal adicional.

##### Edge Cases & Boundary Conditions
JWT es propuesto y aún no está implementado.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |

#### Post-conditions
Corrida 2026-11-13: Partial; evidencia parcial.

#### Traceability
FR-002 | SC-005 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#renterRoleAndForeignOwnerAreForbidden | In Process
