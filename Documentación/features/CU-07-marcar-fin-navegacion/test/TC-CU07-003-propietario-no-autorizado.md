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
La integración con identidades por cabecera comprueba los 403 de arrendatario y propietario ajeno; el plan exige JWT y no se comprueba la ausencia de efectos persistidos.

##### Dependencies & Assumptions
Evidencia V4 de componentes con dependencias controladas; los límites del escenario se detallan en Comments.

##### Preconditions & Default Setup
Reserva IN_NAVIGATION; solicitante distinto del propietario registrado.

#### Test Execution Flow  *(mandatory)*
##### Section 1: Primary Functional Journey
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar | Registrar estado inicial y fijar el reloj cuando corresponda. | Precondiciones | Entorno aislado disponible. | Pasó | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden (XML V4: passed). Aserciones de 403 y PROFILE_NOT_AUTHORIZED para ambos solicitantes; no comprueba JWT ni el estado persistido posterior. |
| 2 | Ejecutar | Solicitar cierre. | Datos de las precondiciones | Según el plan, 403 PROFILE_NOT_AUTHORIZED; sin transición. | Pendiente | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden (XML V4: passed). Aserciones de 403 y PROFILE_NOT_AUTHORIZED para ambos solicitantes; no comprueba JWT ni el estado persistido posterior. |
| 3 | Comprobar | Comparar respuesta y efectos persistidos con el estado inicial. | Reserva y eventos relacionados | Se cumplen los resultados del paso 2. | Pendiente | NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden (XML V4: passed). Aserciones de 403 y PROFILE_NOT_AUTHORIZED para ambos solicitantes; no comprueba JWT ni el estado persistido posterior. |

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
Corrida V4 del 2026-11-13: Partial; estado documental: In Process; evidencia: NavigationEndIntegrationTest#renterRoleAndForeignOwnerAreForbidden. La integración con identidades por cabecera comprueba los 403 de arrendatario y propietario ajeno; el plan exige JWT y no se comprueba la ausencia de efectos persistidos.

#### Traceability
FR-002 | SC-005 | Evidencia: src/test/java/com/seashare/m2/reservas/integration/NavigationEndIntegrationTest.java#renterRoleAndForeignOwnerAreForbidden | In Process
